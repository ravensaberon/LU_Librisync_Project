package com.lulibrisync.repository;

import com.lulibrisync.model.Reservation;
import com.lulibrisync.model.ReservationRequestType;
import com.lulibrisync.model.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findAllByOrderByReservedAtDesc();

    List<Reservation> findByStudent_IdOrderByReservedAtDesc(Long studentId);

    List<Reservation> findByStudent_IdAndRequestTypeOrderByReservedAtDesc(Long studentId, ReservationRequestType requestType);

    List<Reservation> findByBook_IdAndStatusInOrderByQueuePositionAscReservedAtAsc(Long bookId, Collection<ReservationStatus> statuses);

    Optional<Reservation> findFirstByBook_IdAndStatusInOrderByQueuePositionAscReservedAtAsc(Long bookId, Collection<ReservationStatus> statuses);

    boolean existsByBook_IdAndStudent_IdAndStatusIn(Long bookId, Long studentId, Collection<ReservationStatus> statuses);

    boolean existsByBook_IdAndStudent_IdAndStatusInAndRequestType(Long bookId, Long studentId, Collection<ReservationStatus> statuses, ReservationRequestType requestType);

    long countByBook_IdAndStatusIn(Long bookId, Collection<ReservationStatus> statuses);

    long countByBook_IdAndStatusInAndRequestType(Long bookId, Collection<ReservationStatus> statuses, ReservationRequestType requestType);

    List<Reservation> findByStatusInOrderByReservedAtAsc(Collection<ReservationStatus> statuses);

    List<Reservation> findByStatusInAndRequestTypeOrderByReservedAtAsc(Collection<ReservationStatus> statuses, ReservationRequestType requestType);

    List<Reservation> findByStatusAndExpiresAtBefore(ReservationStatus status, LocalDateTime expiresAt);
}
