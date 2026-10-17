package com.fu.cinema.service.impl;

import com.fu.cinema.dto.RegisterForm;
import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.entity.Account;
import com.fu.cinema.entity.Customer;
import com.fu.cinema.enums.AccountStatus;
import com.fu.cinema.enums.Role;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.repository.AccountRepository;
import com.fu.cinema.repository.CustomerRepository;
import com.fu.cinema.service.AuthService;
import com.fu.cinema.util.ValidationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private static final int MIN_REGISTER_AGE = 13;

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public SessionUser login(String username, String password) {
        Account account = accountRepository.findByUsername(username.trim().toLowerCase())
                .filter(a -> passwordEncoder.matches(password, a.getPassword()))
                .orElseThrow(() -> new BusinessException("Tên đăng nhập hoặc mật khẩu không đúng"));

        if (account.getStatus() == AccountStatus.LOCKED) {                       // BR-03
            throw new BusinessException("Tài khoản đã bị khoá. Vui lòng liên hệ quản trị viên");
        }

        if (account.getRole() == Role.ADMIN) {
            return new SessionUser(account.getAccountId(), null, "Administrator", Role.ADMIN);
        }
        Customer customer = customerRepository.findByAccount_AccountId(account.getAccountId())
                .orElseThrow(() -> new BusinessException("Không tìm thấy hồ sơ khách hàng"));
        return new SessionUser(account.getAccountId(), customer.getCustomerId(), customer.getFullName(), Role.CUSTOMER);
    }

    @Override
    @Transactional
    public SessionUser register(RegisterForm form) {
        String email = form.getEmail().trim().toLowerCase();

        if (!form.getPassword().equals(form.getConfirmPassword())) {
            throw new BusinessException("Mật khẩu nhập lại không khớp");
        }
        if (ValidationUtils.age(form.getBirthday()) < MIN_REGISTER_AGE) {            // BR-02
            throw new BusinessException("Bạn phải từ đủ " + MIN_REGISTER_AGE + " tuổi để đăng ký");
        }
        if (accountRepository.existsByUsername(email) || customerRepository.existsByEmail(email)) {   // BR-01
            throw new BusinessException("Email đã được sử dụng");
        }
        if (customerRepository.existsByMobile(form.getMobile())) {
            throw new BusinessException("Số điện thoại đã được sử dụng");
        }
        if (customerRepository.existsByIdentityCard(form.getIdentityCard())) {
            throw new BusinessException("Số CCCD đã được sử dụng");
        }

        Account account = accountRepository.save(new Account(
                email, passwordEncoder.encode(form.getPassword()), Role.CUSTOMER, AccountStatus.ACTIVE));

        Customer customer = new Customer();
        customer.setFullName(form.getFullName().trim());
        customer.setEmail(email);
        customer.setMobile(form.getMobile());
        customer.setBirthday(form.getBirthday());
        customer.setIdentityCard(form.getIdentityCard());
        customer.setAccount(account);
        customerRepository.save(customer);

        return new SessionUser(account.getAccountId(), customer.getCustomerId(), customer.getFullName(), Role.CUSTOMER);
    }
}
