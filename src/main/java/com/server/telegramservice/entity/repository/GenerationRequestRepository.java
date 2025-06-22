package com.server.telegramservice.entity.repository;

import com.server.telegramservice.dto.requests.GenerationRequest;
import com.server.telegramservice.entity.enums.GenerationStatus;
import com.server.telegramservice.entity.enums.MediaType;
import com.server.telegramservice.entity.telegram.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface GenerationRequestRepository extends JpaRepository<GenerationRequest, Long> {

    Optional<GenerationRequest> findByOperationId(String operationId);

    List<GenerationRequest> findByUserOrderByCreatedAtDesc(User user);

    Page<GenerationRequest> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    List<GenerationRequest> findByStatusAndCreatedAtBefore(GenerationStatus status, LocalDateTime before);

    @Query("SELECT gr FROM GenerationRequest gr WHERE gr.user = :user AND gr.status = :status ORDER BY gr.createdAt DESC")
    List<GenerationRequest> findByUserAndStatus(@Param("user") User user, @Param("status") GenerationStatus status);

    @Query("SELECT COUNT(gr) FROM GenerationRequest gr WHERE gr.user = :user AND gr.mediaType = :mediaType AND gr.createdAt >= :since")
    Long countByUserAndMediaTypeAndCreatedAtAfter(@Param("user") User user,
                                                  @Param("mediaType") MediaType mediaType,
                                                  @Param("since") LocalDateTime since);

    @Query("SELECT AVG(gr.rating) FROM GenerationRequest gr WHERE gr.model = :model AND gr.rating IS NOT NULL")
    Double getAverageRatingByModel(@Param("model") String model);

    @Query("SELECT gr.model, COUNT(gr) as count FROM GenerationRequest gr WHERE gr.status = 'COMPLETED' AND gr.createdAt >= :since GROUP BY gr.model ORDER BY count DESC")
    List<Object[]> getPopularModelsSince(@Param("since") LocalDateTime since);

    Optional<GenerationRequest> findTopByUserTelegramIdAndStatusNotInOrderByCreatedAtDesc(
            Long userTelegramId,
            Collection<GenerationStatus> completedStatuses
    );

}
