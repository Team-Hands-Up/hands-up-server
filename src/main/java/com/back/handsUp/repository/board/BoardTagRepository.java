package com.back.handsUp.repository.board;

import com.back.handsUp.domain.board.Board;
import com.back.handsUp.domain.board.BoardTag;
import com.back.handsUp.domain.board.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoardTagRepository extends JpaRepository<BoardTag, Long> {
    List<BoardTag> findAllByBoardIdx(Board boardIdx);
    Optional<BoardTag> findByBoardIdxAndTagIdx(Board boardIdx, Tag tagIdx);
}