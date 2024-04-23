# 목차
🎀 [Spring Data JPA Repository로 변경](#spring-data-jpa-repository로-변경)  
🎀 [사용자 정의 Repository](#사용자-정의-repository)  
🎀 [Spring Data 페이징 활용 1 - QueryDSL 페이징 연동](#spring-data-페이징-활용-1---querydsl-페이징-연동)  
🎀 [Spring Data 페이징 활용 2 - CountQuery 최적화](#spring-data-페이징-활용-2---countquery-최적화)  
🎀 [Spring Data 페이징 활용 3 - 컨트롤러 개발](#spring-data-페이징-활용-3---컨트롤러-개발)  

## Spring Data JPA Repository로 변경
### [Spring Data JPA Repository 생성](..%2Fsrc%2Fmain%2Fjava%2Fstudy%2Fquerydsl%2Frepository%2FMemberRepository.java)
```java
public interface MemberRepository extends JpaRepository<Member, Long> {

    List<Member> findByUsername(String username);
}
```

### [Spring Data JPA Repository 테스트 생성](..%2Fsrc%2Ftest%2Fjava%2Fstudy%2Fquerydsl%2Frepository%2FMemberRepositoryTest.java)
```java
@SpringBootTest
@Transactional
class MemberRepositoryTest {

    @Autowired
    EntityManager em;

    @Autowired
    MemberRepository memberRepository;

    @Test
    void basicTest() {
        Member member = new Member("member1", 10);
        memberRepository.save(member);

        Member findMember = memberRepository.findById(member.getId()).get();
        assertThat(findMember).isEqualTo(member);

        List<Member> result1 = memberRepository.findAll();
        assertThat(result1).containsExactly(member);

        List<Member> result2 = memberRepository.findByUsername("member1");
        assertThat(result2).containsExactly(member);
    }

}
```

`QueryDSL` 전용 기능인 회원 search를 작성할 수 없다. ➡️ **사용자 정의 Repository가 필요하다.**


## Spring Data 페이징 활용 1 - QueryDSL 페이징 연동
## Spring Data 페이징 활용 2 - CountQuery 최적화
## Spring Data 페이징 활용 3 - 컨트롤러 개발
