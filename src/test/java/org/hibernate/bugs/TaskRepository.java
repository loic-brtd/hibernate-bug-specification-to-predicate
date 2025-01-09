/*
 * (c) Copyright 1998-2025, ANS. All rights reserved.
 */

package org.hibernate.bugs;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {
}
