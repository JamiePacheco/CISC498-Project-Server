package com.aux_arena.repository;

import com.aux_arena.models.tables.UserPromptSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserPromptSubmissionRepository extends JpaRepository<UserPromptSubmission, Long> {
}
