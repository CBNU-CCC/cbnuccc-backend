package com.cbnuccc.cbnuccc.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cbnuccc.cbnuccc.Model.Prayer;

public interface PrayerJpaRepository extends JpaRepository<Prayer, Integer> {
}
