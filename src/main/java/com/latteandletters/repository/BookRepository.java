package com.latteandletters.repository;

import com.latteandletters.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {

    List<Book> findAllByOrderByArchivedAscTitleAsc();

    Optional<Book> findByBarcodeIgnoreCaseAndArchivedFalse(String barcode);

    Optional<Book> findByIsbnIgnoreCaseAndArchivedFalse(String isbn);

    List<Book> findByAvailableQuantityGreaterThanAndArchivedFalseOrderByTitleAsc(Integer availableQuantity);

    boolean existsByIsbnIgnoreCase(String isbn);

    boolean existsByBarcodeIgnoreCase(String barcode);

    boolean existsByIsbnIgnoreCaseAndIdNot(String isbn, Long id);

    boolean existsByBarcodeIgnoreCaseAndIdNot(String barcode, Long id);

    long countByAvailableQuantityGreaterThan(Integer availableQuantity);

    long countByCategory_Id(Long categoryId);

    long countByAuthor_Id(Long authorId);

    List<Book> findByArchivedTrueOrderByTitleAsc();

    long countByArchivedTrue();

    long countByVisibleInCatalogTrueAndArchivedFalse();

    @Query("""
            select b from Book b
            where (:keyword is null
                   or lower(b.title) like lower(concat('%', :keyword, '%'))
                   or lower(coalesce(b.barcode, '')) like lower(concat('%', :keyword, '%')))
              and (:categoryId is null or b.category.id = :categoryId)
              and (:authorId is null or b.author.id = :authorId)
              and (:isbn is null or lower(b.isbn) like lower(concat('%', :isbn, '%')))
              and b.archived = false
              and b.visibleInCatalog = true
              and (:availableOnly = false or b.availableQuantity > 0)
            order by b.title asc
            """)
    List<Book> searchBooks(@Param("keyword") String keyword,
                           @Param("categoryId") Long categoryId,
                           @Param("authorId") Long authorId,
                           @Param("isbn") String isbn,
                           @Param("availableOnly") boolean availableOnly);
}
