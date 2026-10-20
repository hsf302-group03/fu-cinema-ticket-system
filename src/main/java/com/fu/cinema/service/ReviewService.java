package com.fu.cinema.service;

import com.fu.cinema.dto.RatingSummary;
import com.fu.cinema.entity.Review;
import com.fu.cinema.dto.ReviewForm;

import java.util.List;

public interface ReviewService {

    List<Review> findByMovie(Long movieId);

    RatingSummary summarize(Long movieId);

    // [C] issue #18 (bonus) thêm canReview, getForm, save
    boolean canReview(Long customerId, Long movieId);

    ReviewForm getForm(Long customerId, Long movieId);

    void save(Long customerId, Long movieId, ReviewForm form);
}
