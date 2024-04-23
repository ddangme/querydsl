# 목차
🎀 [순수 JPA Repository와 QueryDSL](#순수-jpa-repository와-querydsl)
🎀 [동적 쿼리와 성능 최적화 조회](#동적-쿼리와-성능-최적화-조회)
🎀 [조회 API 컨트롤러 개발](#조회-api-컨트롤러-개발)

## 순수 JPA Repository와 QueryDSL
### [순수 JPA Repository - MemberJPARepository](..%2Fsrc%2Fmain%2Fjava%2Fstudy%2Fquerydsl%2Frepository%2FMemberJPARepository.java)
```java
@Repository
public class MemberJPARepository {

    private final EntityManager em;
    private final JPAQueryFactory queryFactory;

    public MemberJPARepository(EntityManager em) {
        this.em = em;
        this.queryFactory = new JPAQueryFactory(em);
    }

    public void save(Member member) {
        em.persist(member);
    }

    public Optional<Member> findById(Long id) {
        Member findMember = em.find(Member.class, id);
        return Optional.ofNullable(findMember);
    }

    public List<Member> findAll() {
        return em.createQuery("select m from Member m", Member.class)
                .getResultList();
    }

    public List<Member> findByUsername(String username) {
        return em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", username)
                .getResultList();
    }
}
```

### [순수 JPA 레포지토리 테스트 - MemberJPARepositoryTest](..%2Fsrc%2Ftest%2Fjava%2Fstudy%2Fquerydsl%2Frepository%2FMemberJPARepositoryTest.java)
```java
@ActiveProfiles("test")
@SpringBootTest
@Transactional
class MemberJPARepositoryTest {

    @Autowired
    EntityManager em;

    @Autowired
    MemberJPARepository memberJpaRepository;


    @Test
    void basicTest() {
        Member member = new Member("member1", 10);
        memberJpaRepository.save(member);
        Member findMember = memberJpaRepository.findById(member.getId()).get();
        assertThat(findMember).isEqualTo(member);
        List<Member> result1 = memberJpaRepository.findAll();
        assertThat(result1).containsExactly(member);
        List<Member> result2 = memberJpaRepository.findByUsername("member1");
        assertThat(result2).containsExactly(member);
    }
}
```

### QueryDSL 사용
#### [순수 JPA Repository에 QueryDSL 추가](..%2Fsrc%2Fmain%2Fjava%2Fstudy%2Fquerydsl%2Frepository%2FMemberJPARepository.java)
```java
public List<Member> findAll_QueryDSL() {
    return queryFactory
            .selectFrom(member).fetch();
}

public List<Member> findByUsername_QueryDSL(String username) {
    return queryFactory
            .selectFrom(member)
            .where(member.username.eq(username))
            .fetch();
}
```

#### QueryDSL 테스트 추가
```java
@Test
void basicQueryDSLTest() {
    Member member = new Member("member1", 10);
    memberJpaRepository.save(member);

    Member findMember = memberJpaRepository.findById(member.getId()).get();
    assertThat(findMember).isEqualTo(member);

    List<Member> result1 = memberJpaRepository.findAll_QueryDSL();
    assertThat(result1).containsExactly(member);

    List<Member> result2 = memberJpaRepository.findByUsername_QueryDSL("member1");
    assertThat(result2).containsExactly(member);
} 
```

### [JPAQueryFactory 스프링 빈 등록 - QuerydslApplication](..%2Fsrc%2Fmain%2Fjava%2Fstudy%2Fquerydsl%2FQuerydslApplication.java)
다음과 같이 `JPAQueryFactory`를 스프링 빈으로 등록해서 주입받아 사용해도 된다.

```java
@Bean
JPAQueryFactory jpaQueryFactory(EntityManager entityManager) {
	return new JPAQueryFactory(entityManager);
}
```

```java
@Repository
@RequiredArgsConstructor
public class MemberJPARepository {

    private final EntityManager em;
    private final JPAQueryFactory queryFactory;
    
    ...
}
```

> 🍀 동시성 문제는 걱정하지 않아도 된다. 왜냐하면 여기서 스프링이 주입해주는 엔티티 매니저는 실제 동작 시점에 진짜 엔티티 매니저를 찾아주는 프록시용 가짜 엔티티 매니저이다.
> 이 가짜 엔티티 매니저는 실제 사용 시점에 트랜잭션 단위로 실제 엔티티 매니저(영속성 컨텍스트)를 할당해준다.

## 동적 쿼리와 성능 최적화 조회
## 조회 API 컨트롤러 개발