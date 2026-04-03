package com.pims.plateform.security;

import com.pims.plateform.entity.User;
import com.pims.plateform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                    new UsernameNotFoundException("Utilisateur non trouvé : " + email)
                );
        return build(user);
    }

    public UserDetails loadUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() ->
                    new UsernameNotFoundException("Utilisateur non trouvé : " + id)
                );
        return build(user);
    }

    private UserDetails build(User user) {
        return org.springframework.security.core.userdetails.User.builder()
                .username(String.valueOf(user.getId()))
                .password(user.getPasswordHash())
                .authorities(List.of(
                    new SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase())
                ))
                .accountLocked(!user.getIsActive())
                .build();
    }
}