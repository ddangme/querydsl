# 목차
🎀 [인터페이스 지원 - QuerydslPredicateExecutor](#인터페이스-지원---querydslpredicateexecutor)
🎀 [Querydsl Web 지원](#querydsl-web-지원)
🎀 [리포지토리 지원 - QuerydslRepositorySupport](#리포지토리-지원---querydslrepositorysupport)
🎀 [Querydsl 지원 클래스 직접 만들기](#querydsl-지원-클래스-직접-만들기)
🎀 [스프링 부트 2.6 이상, QueryDSL 5.0 지원 방법](#스프링-부트-26-이상-querydsl-50-지원-방법)

여기서 소개하는 기능은 제약이 커서 복잡한 실무 환경에서 사용하기에는 많이 부족하다.
그래도 스프링 데이터에서 제공하는 기능이므로 간단하게 소개하고, 왜 부족한지 설명한다.

## 인터페이스 지원 - QuerydslPredicateExecutor
[스프링 공식 문서](https://docs.spring.io/spring-data/jpa/docs/2.2.3.RELEASE/reference/html/#core.extensions.querydsl)

### QuerydslPredicateExecutor 인터페이스
```java
public interface QuerydslPredicateExecutor<T> {
    Optional<T> findById(Predicate predicate);
    Iterable<T> findAll(Predicate predicate);
    long count(Predicate predicate);
    boolean exists(Predicate predicate);
    
    // ... more functionality omitted.
}
```

### 리포지토리에 적용
```java
interface MemberRepository extends JpaRepository<User, Long>, QuerydslPredicateExecutor<User> {  }
```

```java
Iterable result = memberRepository.findAll(member.age.between(10, 40));
```

### 한계점
- 조인 불가 (묵시적 조인은 가능하지만, `LEFT JOIN`이 불가능하다.)
- 클라이언트가 `QueryDSL`에 의존해야 한다. 서비스 클래스가 `QueryDSL`이라는 구현 기술에 의존해야 한다.
- 복잡한 실무환경에서 사용하기에는 한계가 명확하다.

> 🍀 `QuerydslPredicateExecutor`는 `Pageable`, `Sort`를 모두 지원하고 정상 동작한다.

## Querydsl Web 지원
[스프링 공식 문서](https://docs.spring.io/spring-data/jpa/docs/2.2.3.RELEASE/reference/html/#core.web.type-safe)

### 한계점
- 단순한 조건만 가능하다.
- 조건을 커스텀하는 기능이 복잡하고 명시적이지 않다.
- 컨트롤러가 `QueryDSL`에 의존한다.
- 복잡한 실무환경에서 사용하기에는 한계가 명확하다.

## 리포지토리 지원 - QuerydslRepositorySupport
### 장점
- `getQuerydsl().applyPagination()` 스프링 데이터가 제공하는 페이징을 Querydsl로 편리하게 변환이 가능하다. (단! Sort는 오류발생)
- `from()` 으로 시작 가능(최근에는 QueryFactory를 사용해서 `select()` 로 시작하는 것이 더 명시적)
- EntityManager 제공

### 한계점
- Querydsl 3.x 버전을 대상으로 만들어 졌다.
- Querydsl 4.x에 나온 JPAQueryFactory로 시작할 수 없음
  - select로 시작할 수 없음 (from으로 시작해야함) 
- `QueryFactory` 를 제공하지 않음
- 스프링 데이터 Sort 기능이 정상 동작하지 않음

## Querydsl 지원 클래스 직접 만들기
스프링 데이터가 제공하는 `QuerydslRepositorySupport`가 지닌 한계를 극복하기 위해 직접 `QueryDSL 지원 클래스`를 만들어 보자.
### 장점
- 스프링 데이터가 제공하는 페이징을 편리하게 변환 
- 페이징과 카운트 쿼리 분리 가능
- 스프링 데이터 Sort 지원
- `select()` , `selectFrom()` 으로 시작 가능
- `EntityManager` , `QueryFactory` 제공

### 지원 클래스 생성
```java
package study.querydsl.repository.support;

import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.JpaEntityInformationSupport;
import org.springframework.data.jpa.repository.support.Querydsl;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.data.repository.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import java.util.List;
import java.util.function.Function;
/**
* Querydsl 4.x 버전에 맞춘 Querydsl 지원 라이브러리 *
* @author Younghan Kim
* @see
org.springframework.data.jpa.repository.support.QuerydslRepositorySupport
*/
@Repository
public abstract class Querydsl4RepositorySupport {
    
    private final Class domainClass;
    private Querydsl querydsl;
    private EntityManager entityManager;
    private JPAQueryFactory queryFactory;
    
    public Querydsl4RepositorySupport(Class<?> domainClass) {
        Assert.notNull(domainClass, "Domain class must not be null!");
        this.domainClass = domainClass;
    }
    
    @Autowired
    public void setEntityManager(EntityManager entityManager) {
        Assert.notNull(entityManager, "EntityManager must not be null!");
        JpaEntityInformation entityInformation = JpaEntityInformationSupport.getEntityInformation(domainClass, entityManager);
        SimpleEntityPathResolver resolver = SimpleEntityPathResolver.INSTANCE;
        EntityPath path = resolver.createPath(entityInformation.getJavaType());
        this.entityManager = entityManager;
        this.querydsl = new Querydsl(entityManager, new PathBuilder<>(path.getType(), path.getMetadata()));
        this.queryFactory = new JPAQueryFactory(entityManager);
    }
    
    @PostConstruct
    public void validate() {
        Assert.notNull(entityManager, "EntityManager must not be null!");
        Assert.notNull(querydsl, "Querydsl must not be null!");
        Assert.notNull(queryFactory, "QueryFactory must not be null!");
    }
    
    protected JPAQueryFactory getQueryFactory() {
        return queryFactory;
    }
    
    protected Querydsl getQuerydsl() {
        return querydsl;
    }
    
    protected EntityManager getEntityManager() {
        return entityManager;
    }
    
    protected <T> JPAQuery<T> select(Expression<T> expr) {
        return getQueryFactory().select(expr);
    }
    
    protected <T> JPAQuery<T> selectFrom(EntityPath<T> from) {
        return getQueryFactory().selectFrom(from);
    }
    
    protected <T> Page<T> applyPagination(Pageable pageable, Function<JPAQueryFactory, JPAQuery> contentQuery) {
        JPAQuery jpaQuery = contentQuery.apply(getQueryFactory());
        List<T> content = getQuerydsl().applyPagination(pageable, jpaQuery).fetch();
        
        return PageableExecutionUtils.getPage(content, pageable, jpaQuery::fetchCount);
    }
    
    protected <T> Page<T> applyPagination(Pageable pageable, Function<JPAQueryFactory, JPAQuery> contentQuery, Function<JPAQueryFactory, JPAQuery> countQuery) {
        JPAQuery jpaContentQuery = contentQuery.apply(getQueryFactory());
        List<T> content = getQuerydsl().applyPagination(pageable, jpaContentQuery).fetch();
        JPAQuery countResult = countQuery.apply(getQueryFactory());
        
        return PageableExecutionUtils.getPage(content, pageable, countResult::fetchCount);
    } 
}
```

### 사용 코드
```java
package study.querydsl.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.entity.Member;
import study.querydsl.repository.support.Querydsl4RepositorySupport;
import java.util.List;

import static org.springframework.util.StringUtils.isEmpty;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

@Repository
public class MemberTestRepository extends Querydsl4RepositorySupport {
    public MemberTestRepository() {
        super(Member.class);
    }
    
    public List<Member> basicSelect() {
        return select(member)
                .from(member)
                .fetch();
    }
    
    public List<Member> basicSelectFrom() {
        return selectFrom(member)
                .fetch();
    }
    
    public Page<Member> searchPageByApplyPage(MemberSearchCondition condition, Pageable pageable) {
        JPAQuery<Member> query = selectFrom(member)
                .leftJoin(member.team, team)
                .where(usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe()));
        
        List<Member> content = getQuerydsl().applyPagination(pageable, query).fetch();
        
        return PageableExecutionUtils.getPage(content, pageable, query::fetchCount);
    }
    
    public Page<Member> applyPagination(MemberSearchCondition condition, Pageable pageable) {
        return applyPagination(pageable, contentQuery -> contentQuery
                .selectFrom(member)
                .leftJoin(member.team, team)
                .where(usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())));
    }
    
    public Page<Member> applyPagination2(MemberSearchCondition condition, Pageable pageable) {
        return applyPagination(pageable, contentQuery -> contentQuery
                        .selectFrom(member)
                        .leftJoin(member.team, team)
                        .where(usernameEq(condition.getUsername()),
                                teamNameEq(condition.getTeamName()),
                                ageGoe(condition.getAgeGoe()),
                                ageLoe(condition.getAgeLoe())),
                countQuery -> countQuery
                        .selectFrom(member)
                        .leftJoin(member.team, team)
                        .where(usernameEq(condition.getUsername()),
                                teamNameEq(condition.getTeamName()),
                                ageGoe(condition.getAgeGoe()),
                                ageLoe(condition.getAgeLoe()))
        );
    }
    
    private BooleanExpression usernameEq(String username) {
        return isEmpty(username) ? null : member.username.eq(username);
    }
    
    private BooleanExpression teamNameEq(String teamName) {
        return isEmpty(teamName) ? null : team.name.eq(teamName);
    }
    
    private BooleanExpression ageGoe(Integer ageGoe) {
        return ageGoe == null ? null : member.age.goe(ageGoe);
    }
    
    private BooleanExpression ageLoe(Integer ageLoe) {
        return ageLoe == null ? null : member.age.loe(ageLoe);
    }
}
```

## 스프링 부트 2.6 이상, QueryDSL 5.0 지원 방법
Querydsl의 `fetchCount()` , `fetchResult()` 는 개발자가 작성한 select 쿼리를 기반으로 count용 쿼리를 내부에서 만들어서 실행합니다.  
그런데 이 기능은 강의에서 설명드린 것 처럼 select 구문을 단순히 count 처리하는 용도로 바꾸는 정도입니다. 
따라서 단순한 쿼리에서는 잘 동작하지만, 복잡한 쿼리에서는 제대로 동작하지 않습니다.  
Querydsl은 향후 `fetchCount()` , `fetchResult()` 를 지원하지 않기로 결정했습니다.  
참고로 Querydsl의 변화가 빠르지는 않기 때문에 당장 해당 기능을 제거하지는 않을 것입니다.  


따라서 count 쿼리가 필요하면 다음과 같이 별도로 작성해야 합니다.

### count 쿼리 예제
```java
@Test
void count() {
    Long totalCount = queryFactory
            .select(member.count())
            .from(member)
            .fetchOne();
}
```
- `count(*)` 을 사용하고 싶으면 `.select(Wildcard.count) 를 사용하면 됩니다.
- `member.count()` 를 사용하면 `count(member.id)` 로 처리됩니다. 
- 응답 결과는 숫자 하나이므로 `fetchOne()` 을 사용합니다.

### 최신 버전 예제
`MemberRepositoryImpl.searchPageComplex()` 예제에서 보여드린 것 처럼 select 쿼리와는 별도로 count 쿼리를 작성하고 `fetch()` 를 사용해야 합니다.
```java
@Override
public Page<MemberTeamDTO> searchPageComplex2(MemberSearchCondition condition, Pageable pageable) {
    List<MemberTeamDTO> content = queryFactory
            .select(new QMemberTeamDTO(
                    member.id.as("memberId"),
                    member.username,
                    member.age,
                    team.id.as("teamId"), team.name.as("teamName")))
            .from(member)
            .leftJoin(member.team, team)
            .where(
                    usernameEq(condition.getUsername()),
                    teamNameEq(condition.getTeamName()),
                    ageGoe(condition.getAgeGoe()),
                    ageLoe(condition.getAgeLoe())
            )
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

    JPAQuery<Long> countQuery = queryFactory
            .select(member.count())
            .from(member)
            .leftJoin(member.team, team)
            .where(
                    usernameEq(condition.getUsername()),
                    teamNameEq(condition.getTeamName()),
                    ageGoe(condition.getAgeGoe()),
                    ageLoe(condition.getAgeLoe())
            );

    return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
}
```