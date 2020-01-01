package com.upgrad.quora.service.business;

import com.upgrad.quora.service.dao.UserDao;
import com.upgrad.quora.service.entity.UserAuthEntity;
import com.upgrad.quora.service.entity.UserEntity;
import com.upgrad.quora.service.exception.AuthenticationFailedException;
import com.upgrad.quora.service.exception.SignOutRestrictedException;
import com.upgrad.quora.service.exception.SignUpRestrictedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.UUID;

@Service
public class UserBusinessService {

    @Autowired
    private UserDao userDao;

    @Autowired
    private PasswordCryptographyProvider cryptographyProvider;

    @Transactional(propagation = Propagation.REQUIRED)
    public UserEntity createUser(UserEntity userEntity) throws SignUpRestrictedException {

        UserEntity username = userDao.getUserByUsername(userEntity.getUsername());
        UserEntity email = userDao.getUserByEmail(userEntity.getEmail());

        if (username != null) {

            throw new SignUpRestrictedException("SGR-001", "Try any other Username, this Username has already been taken");

        }
        if (email != null) {
            throw new SignUpRestrictedException("SGR-002", "This user has already been registered, try with any other emailId");

        } else {

            String encryptedText[] = cryptographyProvider.encrypt(userEntity.getPassword());
            userEntity.setSalt(encryptedText[0]);
            userEntity.setPassword(encryptedText[1]);
            return userDao.createUser(userEntity);
        }

    }

    @Transactional(propagation = Propagation.REQUIRED)
    public UserAuthEntity userAuthentication(String username, String password) throws AuthenticationFailedException {

        UserEntity userEntity = userDao.getUserByUsername(username);

        if (userEntity == null) {
            throw new AuthenticationFailedException("ATH-001", "This username does not exist");

        }

        String encryptedPassword = cryptographyProvider.encrypt(password, userEntity.getSalt());

        if (userEntity.getPassword().equals(encryptedPassword)) {
            JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(encryptedPassword);
            UserAuthEntity userAuthEntity = new UserAuthEntity();
            userAuthEntity.setUser(userEntity);
            userAuthEntity.setUuid(UUID.randomUUID().toString());
            final ZonedDateTime now = ZonedDateTime.now();
            final ZonedDateTime expiresAt = now.plusHours(10);
            userAuthEntity.setAccessToken(jwtTokenProvider.generateToken(userEntity.getUuid(), now, expiresAt));
            userAuthEntity.setExpiresAt(expiresAt);
            userAuthEntity.setLoginAt(now);
            userDao.createToken(userAuthEntity);
            return userAuthEntity;
        } else {
            throw new AuthenticationFailedException("ATH-002", "Password failed");
        }

    }





}