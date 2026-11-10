package com.fu.cinema.desktop.model;

import com.fu.cinema.entity.Customer;

import java.time.LocalDate;
import java.time.Period;

/**
 * Record đại diện cho khách hàng hiển thị trong danh sách chọn (TODO 43).
 */
public record CustomerItem(Long id, String fullName, String mobile, int age) {

    public static CustomerItem from(Customer c) {
        int age = c.getBirthday() != null
                ? Period.between(c.getBirthday(), LocalDate.now()).getYears()
                : 0;
        return new CustomerItem(c.getCustomerId(), c.getFullName(), c.getMobile(), age);
    }

    @Override
    public String toString() {
        return fullName + " – " + mobile + " – " + age + " tuổi";
    }
}

