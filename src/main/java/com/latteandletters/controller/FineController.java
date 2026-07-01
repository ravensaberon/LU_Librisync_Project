package com.latteandletters.controller;

import com.latteandletters.model.Fine;
import com.latteandletters.model.FineStatus;
import com.latteandletters.service.FineService;
import com.latteandletters.util.PaginationUtils;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Controller
@RequestMapping("/admin/fines")
public class FineController {

    private static final int FINE_LEDGER_PAGE_SIZE = 10;

    private final FineService fineService;

    public FineController(FineService fineService) {
        this.fineService = fineService;
    }

    @GetMapping
    public String fines(@RequestParam(required = false) FineStatus status,
                        @RequestParam(required = false) String studentKeyword,
                        @RequestParam(defaultValue = "1") Integer page,
                        Model model) {
        List<Fine> fineRecords = fineService.getAllFines().stream()
                .filter(fine -> status == null || fine.getStatus() == status)
                .filter(fine -> matchesStudentFilter(fine, studentKeyword))
                .toList();
        var finesPage = PaginationUtils.paginate(fineRecords, page, FINE_LEDGER_PAGE_SIZE);

        BigDecimal filteredOutstandingTotal = fineRecords.stream()
                .filter(fine -> FineStatus.UNPAID.equals(fine.getStatus()))
                .map(Fine::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("fines", finesPage.getItems());
        model.addAttribute("finesPage", finesPage);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("studentKeyword", studentKeyword);
        model.addAttribute("fineStatuses", FineStatus.values());
        model.addAttribute("outstandingFineCount", fineService.countByStatus(FineStatus.UNPAID));
        model.addAttribute("paidFineCount", fineService.countByStatus(FineStatus.PAID));
        model.addAttribute("waivedFineCount", fineService.countByStatus(FineStatus.WAIVED));
        model.addAttribute("outstandingFineTotal", fineService.getTotalAmountByStatus(FineStatus.UNPAID));
        model.addAttribute("paidFineTotal", fineService.getTotalAmountByStatus(FineStatus.PAID));
        model.addAttribute("waivedFineTotal", fineService.getTotalAmountByStatus(FineStatus.WAIVED));
        model.addAttribute("filteredOutstandingTotal", filteredOutstandingTotal);
        model.addAttribute("filteredFineCount", fineRecords.size());
        return "admin/fines";
    }

    @PostMapping("/{fineId}/pay")
    public String markFinePaid(@PathVariable Long fineId,
                               @RequestParam(defaultValue = "1") Integer page,
                               @RequestParam(required = false) FineStatus status,
                               @RequestParam(required = false) String studentKeyword,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            fineService.markFinePaid(fineId, authentication.getName());
            redirectAttributes.addFlashAttribute("success", "Fine marked as paid successfully.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return buildFineRedirect(page, status, studentKeyword);
    }

    @PostMapping("/{fineId}/waive")
    public String waiveFine(@PathVariable Long fineId,
                            @RequestParam(defaultValue = "1") Integer page,
                            @RequestParam(required = false) FineStatus status,
                            @RequestParam(required = false) String studentKeyword,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        try {
            fineService.waiveFine(fineId, authentication.getName());
            redirectAttributes.addFlashAttribute("success", "Fine waived successfully.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return buildFineRedirect(page, status, studentKeyword);
    }

    private boolean matchesStudentFilter(Fine fine, String studentKeyword) {
        if (studentKeyword == null || studentKeyword.isBlank()) {
            return true;
        }

        String normalizedKeyword = studentKeyword.trim().toLowerCase(Locale.ENGLISH);
        return fine.getStudent().getStudentId().toLowerCase(Locale.ENGLISH).contains(normalizedKeyword)
                || fine.getStudent().getUser().getName().toLowerCase(Locale.ENGLISH).contains(normalizedKeyword)
                || fine.getStudent().getUser().getEmail().toLowerCase(Locale.ENGLISH).contains(normalizedKeyword);
    }

    private String buildFineRedirect(Integer page, FineStatus status, String studentKeyword) {
        StringBuilder redirect = new StringBuilder("redirect:/admin/fines?page=")
                .append(page == null ? 1 : Math.max(1, page));
        if (status != null) {
            redirect.append("&status=").append(status.name());
        }
        if (studentKeyword != null && !studentKeyword.isBlank()) {
            redirect.append("&studentKeyword=").append(studentKeyword.trim());
        }
        return redirect.toString();
    }
}
