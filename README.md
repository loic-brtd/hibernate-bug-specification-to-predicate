# Reproduction of possible bug in `spring-data-jpa` / `hibernate`

Tests are located [here](src/test/java/org/hibernate/bugs/MatchAllSpecificationBugTest.java).

The idea is to create a "match all" `org.springframework.data.jpa.domain.Specification` (a specification without any restriction).
When I use `Specification.where(null)`, I get a `NullPointerException` in Hibernate code when executing the query:

```java
Specification<Task> matchAllSpecification = Specification.where(null);

CriteriaBuilder cb = entityManager.getCriteriaBuilder();
CriteriaQuery<Task> query = cb.createQuery(Task.class);

Root<Task> root = query.from(Task.class);
query.where(matchAllSpecification.toPredicate(root, query, cb));

entityManager.createQuery(query).getResultList();
```

Stack trace :
```
java.lang.NullPointerException: Cannot invoke "org.hibernate.query.sqm.tree.expression.SqmExpression.getExpressible()" because "booleanExpression" is null

	at org.hibernate.query.sqm.tree.predicate.SqmBooleanExpressionPredicate.<init>(SqmBooleanExpressionPredicate.java:38)
	at org.hibernate.query.sqm.tree.predicate.SqmBooleanExpressionPredicate.<init>(SqmBooleanExpressionPredicate.java:31)
	at org.hibernate.query.sqm.internal.SqmCriteriaNodeBuilder.wrap(SqmCriteriaNodeBuilder.java:497)
	at org.hibernate.query.sqm.tree.select.SqmQuerySpec.setRestriction(SqmQuerySpec.java:362)
	at org.hibernate.query.sqm.tree.select.AbstractSqmSelectQuery.where(AbstractSqmSelectQuery.java:315)
	at org.hibernate.query.sqm.tree.select.SqmSelectStatement.where(SqmSelectStatement.java:373)
	at org.hibernate.query.sqm.tree.select.SqmSelectStatement.where(SqmSelectStatement.java:46)
	at org.hibernate.bugs.MatchAllSpecificationBugTest.findAllTasks(JPAUnitTestCase.java:69)
	at org.hibernate.bugs.MatchAllSpecificationBugTest.whereNullFailingTest(JPAUnitTestCase.java:49)
```

Argument of `Specification#where(Specification<T>)` is marked as `@Nullable` and using this specification with a `JpaRepository` / `JpaSpecificationExecutor` (`taskRepository.findAll(Specification.where(null))`) seems to work fine as a "match all" query.

This workaround works:

```java
Specification<Task> matchAllSpecification = (root, query, cb) -> cb.conjunction();
```
