package com.fu.cinema.service.impl;

import com.fu.cinema.dto.RatingSummary;
import com.fu.cinema.entity.Review;
import com.fu.cinema.repository.ReviewRepository;
import com.fu.cinema.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;

    @Override
    public List<Review> findByMovie(Long movieId) {
        return reviewRepository.findByMovie_MovieIdOrderByReviewDateDesc(movieId);
    }

    @Override
    public RatingSummary summarize(Long movieId) {
        return reviewRepository.summarize(movieId);
    }
}
