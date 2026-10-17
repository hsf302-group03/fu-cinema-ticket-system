package com.fu.cinema.repository;

import com.fu.cinema.entity.Review;
import com.fu.cinema.entity.ReviewId;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, ReviewId> {

    // ===== Derived query (mục 9.1) =====
    @EntityGraph(attributePaths = "customer")
    List<Review> findByMovie_MovieIdOrderByReviewDateDesc(Long movieId);

    // [C] issue #13 thêm Q10 summarize(...)
}
