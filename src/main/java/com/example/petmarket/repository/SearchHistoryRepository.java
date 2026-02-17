package com.example.petmarket.repository;

import com.example.petmarket.entity.SearchHistory;
import com.example.petmarket.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {

    Optional<SearchHistory> findByUserAndQueryIgnoreCase(User user, String query);

    Optional<SearchHistory> findBySessionIdAndQueryIgnoreCase(String sessionId, String query);

    @Query("SELECT LOWER(sh.query), SUM(sh.searchCount) as totalCount FROM SearchHistory sh " +
            "WHERE LOWER(sh.query) LIKE LOWER(CONCAT(:prefix, '%')) " +
            "GROUP BY LOWER(sh.query) " +
            "ORDER BY totalCount DESC")
    List<Object[]> findPopularSearchesByPrefix(@Param("prefix") String prefix, Pageable pageable);

    @Query("SELECT sh FROM SearchHistory sh " +
            "WHERE sh.user = :user " +
            "AND LOWER(sh.query) LIKE LOWER(CONCAT(:prefix, '%')) " +
            "ORDER BY sh.updatedAt DESC")
    List<SearchHistory> findUserSearchHistoryByPrefix(@Param("user") User user, @Param("prefix") String prefix, Pageable pageable);

    @Query("SELECT sh FROM SearchHistory sh " +
            "WHERE sh.sessionId = :sessionId " +
            "AND LOWER(sh.query) LIKE LOWER(CONCAT(:prefix, '%')) " +
            "ORDER BY sh.updatedAt DESC")
    List<SearchHistory> findSessionSearchHistoryByPrefix(@Param("sessionId") String sessionId, @Param("prefix") String prefix, Pageable pageable);
}