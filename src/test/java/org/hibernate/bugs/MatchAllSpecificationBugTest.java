package org.hibernate.bugs;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

/**
 * Based on this template : https://github.com/hibernate/hibernate-test-case-templates/tree/main/orm/hibernate-orm-6
 */
@EnableJpaRepositories
class MatchAllSpecificationBugTest {

	private EntityManagerFactory entityManagerFactory;
	private EntityManager entityManager;
	private TaskRepository taskRepository;

	@BeforeEach
	void init() {
		entityManagerFactory = Persistence.createEntityManagerFactory("templatePU");
		entityManager = entityManagerFactory.createEntityManager();
		taskRepository = new JpaRepositoryFactory(entityManager).getRepository(TaskRepository.class);

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
	@DisplayName("`Specification.where(null)` fails with `CriteriaBuilder`")
	void specificationWhereNull_criteriaBuilder() {
		// Argument of `Specification#where(Specification<T>)` is marked as `@Nullable` but this test fails with the following exception :
		// java.lang.NullPointerException: Cannot invoke "org.hibernate.query.sqm.tree.expression.SqmExpression.getExpressible()" because "booleanExpression" is null
		Specification<Task> matchAllSpecification = Specification.where(null);

		List<Task> tasks = findWithCriteriaBuilder(matchAllSpecification);

		assertThat(tasks).hasSize(3);
	}

	@Test
	@DisplayName("`(root, query, cb) -> cb.conjunction()` works with `CriteriaBuilder`")
	void conjunctionWorkaround() {
		Specification<Task> matchAllSpecification = (root, query, cb) -> cb.conjunction();

		List<Task> tasks = findWithCriteriaBuilder(matchAllSpecification);

		assertThat(tasks).hasSize(3);
	}

	@Test
	@DisplayName("`Specification.where(null)` works with `JpaRepository` / `JpaSpecificationExecutor`")
	void specificationWhereNull_jpaRepository() {
		Specification<Task> matchAllSpecification = Specification.where(null);

		List<Task> tasks = taskRepository.findAll(matchAllSpecification);

		assertThat(tasks).hasSize(3);
	}

	public List<Task> findWithCriteriaBuilder(Specification<Task> specification) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Task> query = cb.createQuery(Task.class);

		Root<Task> root = query.from(Task.class);
		query.where(specification.toPredicate(root, query, cb));

		return entityManager.createQuery(query).getResultList();
	}

}
