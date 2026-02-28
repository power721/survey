package cn.har01d.survey.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import cn.har01d.survey.entity.SocialAccount;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {
    Optional<SocialAccount> findByProviderAndProviderId(SocialAccount.Provider provider, String providerId);
}
