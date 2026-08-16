package com.cbnuccc.cbnuccc.Service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;

import com.cbnuccc.cbnuccc.Repository.UserJpaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MyUserDetailsService implements UserDetailsService {
    private final UserJpaRepository userJpaRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        var result = userJpaRepository.findByEmail(email.toLowerCase());
        if (result.isEmpty())
            throw new UsernameNotFoundException("There's no given user.");

        var user = result.get();

        List<GrantedAuthority> authsList = new ArrayList<>();
        authsList.add(new SimpleGrantedAuthority("ROLE_" + user.getRank().toString()));

        return new User(user.getEmail(), user.getPassword(), authsList);
    }
}
