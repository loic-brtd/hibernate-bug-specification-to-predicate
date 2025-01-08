package org.hibernate.bugs;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

/**
 * This template demonstrates how to develop a test case for Hibernate ORM, using the Java Persistence API.
 */
class JPAUnitTestCase {

	private EntityManagerFactory entityManagerFactory;
	private EntityManager entityManager;

	@BeforeEach
	void init() {
		entityManagerFactory = Persistence.createEntityManagerFactory("templatePU");
		entityManager = entityManagerFactory.createEntityManager();
		entityManager.getTransaction().begin();

		List.of(new Task(), new Task(), new Task()).forEach(entityManager::persist);
	}

	@AfterEach
	void destroy() {
		entityManager.getTransaction().commit();
		entityManager.close();
		entityManagerFactory.close();
	}

	@Test
	@DisplayName("Failing test using `Specification.where(null)` to create a 'match all' Specification")
	void failingTest() {
		// Argument of `Specification#where(Specification<T>)` is marked as `@Nullable` but this test fails with the following exception :
		// java.lang.NullPointerException: Cannot invoke "org.hibernate.query.sqm.tree.expression.SqmExpression.getExpressible()" because "booleanExpression" is null
		Specification<Task> matchAllSpecification = Specification.where(null);

		List<Task> tasks = findTasks(matchAllSpecification);

		assertThat(tasks).hasSize(3);
	}

	@Test
	@DisplayName("Successful workaround using `(root, query, cb) -> cb.conjunction()` to create a 'match all' Specification")
	void successfulWorkaround() {
		Specification<Task> matchAllSpecification = (root, query, cb) -> cb.conjunction();

		List<Task> tasks = findTasks(matchAllSpecification);

		assertThat(tasks).hasSize(3);
	}

	public List<Task> findTasks(Specification<Task> specification) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Task> query = cb.createQuery(Task.class);

		Root<Task> root = query.from(Task.class);
		query.where(specification.toPredicate(root, query, cb));

		return entityManager.createQuery(query).getResultList();
	}

}
