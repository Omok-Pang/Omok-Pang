# Omok-Pang

### 개발 시작하기 전

1️⃣ 먼저, 인델리제이에서 git clone하기

2️⃣ 자신이 작업할 공간 만들기 : 브랜치 생성하고 이동하기

```markdown
git checkout -b 작업브랜치  #브랜치 생성하고 이동함.
```

3️⃣ 이제 자신이 작업한 것들 커밋하기

```markdown
git add .
git commit -m "커밋 메시지"
```

4️⃣ 이제 깃허브에 PR 올리기

```markdown
git push 
```

---

### 다른 사람이 merge했을때

자신의 작업물을 커밋하고 난 뒤에 머지하러 가기!!!!

```markdown
git checkout main. // main으로 이동
git pull origin main  //깃허브에 있는 최신 업데이트된 main의 내용물 pull 받기
git checkout #자신의 브랜치   //자신의 브랜치로 이동
git merge main   // 자신의 작업 브랜치도 최신 업데이트하기
```







