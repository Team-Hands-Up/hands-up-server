package com.back.handsUp.repository.board;

import com.back.handsUp.domain.board.Board;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {

    Optional<Board> findByBoardIdx(Long boardIdx);

    @Query("select b from Board b where b.status = ?1")
    List<Board> findBoardByStatus(String status);

}
