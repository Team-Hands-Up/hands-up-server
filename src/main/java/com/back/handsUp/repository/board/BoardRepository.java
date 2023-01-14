package com.back.handsUp.repository.board;

import com.back.handsUp.domain.board.Board;
import com.back.handsUp.domain.board.BoardUser;
import com.back.handsUp.domain.user.User;
import com.back.handsUp.mapper.BoardMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.Optional;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {

//    private JdbcTemplate jdbcTemplate;
//
//    @Autowired
//    public void setDataSource(DataSource dataSource) {
//        this.jdbcTemplate = new JdbcTemplate(dataSource);
//    }
//
//    public Board boardViewByIdx(int idx) {
//        String sql = "SELECT * FROM board WHERE boardIdx = ?";
//        Board board = jdbcTemplate.queryForObject(sql, new Object[]{idx},new BoardMapper());
//        return board;
//    }
//
//    public User findUserByBoardIdx(int idx) {
//        String sql = "SELECT B.memberId FROM board B INNER JOIN member M ON B.memberId = M.memberId WHERE B.boardId = ?;";
//        User user = jdbcTemplate.queryForObject(sql, new RowMapper<User>(), idx);
//        return user;
//    }
//
//    public void likeBoard(int userIdx, int boardIdx) {
//        String sql = "INSERT INTO board_user(userIdx, boardIdx, status) VALUES (?,?,?);";
//        BoardUser boardUser = jdbcTemplate.update(sql, userIdx, boardIdx, "heart");
//    }
    Optional<Board> findByBoardIdx(Long boardIdx);



}