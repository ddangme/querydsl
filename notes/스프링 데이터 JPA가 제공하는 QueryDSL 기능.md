# 목차
🎀 [인터페이스 지원 - QuerydslPredicateExecutor](#인터페이스-지원---querydslpredicateexecutor)
🎀 [Querydsl Web 지원](#querydsl-web-지원)
🎀 [리포지토리 지원 - QuerydslRepositorySupport](#리포지토리-지원---querydslrepositorysupport)
🎀 [Querydsl 지원 클래스 직접 만들기](#querydsl-지원-클래스-직접-만들기)

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

한계점
- 단순한 조건만 가능하다.
- 조건을 커스텀하는 기능이 복잡하고 명시적이지 않다.
- 컨트롤러가 `QueryDSL`에 의존한다.
- 복잡한 실무환경에서 사용하기에는 한계가 명확하다.

## 리포지토리 지원 - QuerydslRepositorySupport
## Querydsl 지원 클래스 직접 만들기