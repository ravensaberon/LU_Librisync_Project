package com.latteandletters.repository;

import com.latteandletters.model.IssueRecord;
import com.latteandletters.model.IssueStatus;
import com.latteandletters.repository.projection.BookBorrowStat;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface IssueRecordRepository extends JpaRepository<IssueRecord, Long> {

    List<IssueRecord> findTop8ByOrderByIssueDateDesc();

    List<IssueRecord> findAllByOrderByIssueDateDesc();

    List<IssueRecord> findByStudent_IdOrderByIssueDateDesc(Long studentId);

    List<IssueRecord> findByStatusInOrderByIssueDateDesc(Collection<IssueStatus> statuses);

    long countByStatus(IssueStatus status);

    long countByIssuedBy_EmailIgnoreCase(String email);

    long countByBook_IdAndStatusIn(Long bookId, Collection<IssueStatus> statuses);

    long countByStudent_IdAndStatusIn(Long studentId, Collection<IssueStatus> statuses);

    boolean existsByBook_IdAndStudent_IdAndStatusIn(Long bookId, Long studentId, Collection<IssueStatus> statuses);

    List<IssueRecord> findByBook_IdInAndStatusInOrderByDueDateAsc(Collection<Long> bookIds, Collection<IssueStatus> statuses);

    @Query("""
            select ir.book.title as title, count(ir) as borrowCount
            from IssueRecord ir
            group by ir.book.id, ir.book.title
            order by count(ir) desc, ir.book.title asc
            """)
    List<BookBorrowStat> findMostBorrowedBooks(Pageable pageable);

    @Query("""
            select ir from IssueRecord ir
            where ir.status in :statuses
            order by ir.dueDate asc
            """)
    List<IssueRecord> findActiveIssuesOrdered(@Param("statuses") Collection<IssueStatus> statuses);
}
