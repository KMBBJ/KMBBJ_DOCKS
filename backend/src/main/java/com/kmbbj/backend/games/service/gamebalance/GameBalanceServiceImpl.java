package com.kmbbj.backend.games.service.gamebalance;
import com.kmbbj.backend.auth.entity.User;


import com.kmbbj.backend.feature.exchange.controller.response.TransactionsResponse;
import com.kmbbj.backend.feature.exchange.entity.TransactionType;
import com.kmbbj.backend.feature.exchange.repository.postgre.TransactionRepository;
import com.kmbbj.backend.games.dto.GameBalanceDTO;
import com.kmbbj.backend.games.entity.Game;
import com.kmbbj.backend.games.entity.GameBalance;
import com.kmbbj.backend.games.repository.CoinBalanceRepository;
import com.kmbbj.backend.games.repository.GameBalanceRepository;

import com.kmbbj.backend.games.util.GameEncryptionUtil;
import com.kmbbj.backend.global.config.exception.ApiException;
import com.kmbbj.backend.global.config.exception.ExceptionEnum;
import com.kmbbj.backend.matching.entity.UserRoom;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GameBalanceServiceImpl implements GameBalanceService {

    // 게임 계좌 레퍼지토리
    private final GameBalanceRepository gameBalanceRepository;
    private final CoinBalanceRepository coinBalanceRepository;
    private final GameEncryptionUtil gameEncryptionUtil;
    private final TransactionRepository transactionRepository;

    /**
     * 게임 시작 시 호출 , 방에 설정된 시드머니 초기 잔액 사용
     *
     * @param game 잔액을 생성할 게임 객체
     * @return 생성된 GameBalance 객체
     */
    @Override
    @Transactional
    public List<GameBalance> createGameBalance(Game game) {
        // 방의 시작 시드머니 가져옴
        Long seedMoney = Long.valueOf((String.valueOf(game.getRoom().getStartSeedMoney().getAmount())));


        // 실제로 게임을 플레이한 사용자 리스트 가져오기
        List<User> participants = game.getRoom().getUserRooms().stream()
                .filter(UserRoom::getIsPlayed) // 실제로 플레이한 사용자 필터링
                .map(UserRoom::getUser) // UserRoom -> User 객체 추출
                .collect(Collectors.toList());

        // 각 사용자에 대한 게임 잔액 생성
        List<GameBalance> gameBalances = new ArrayList<>();

        // 각 사용자에 대한 게임 잔액 생성
        for (User user : participants) {
            GameBalance gameBalance = new GameBalance();
            gameBalance.setGame(game);  // 게임 정보 설정
            gameBalance.setUser(user);  // 사용자 정보 설정
            gameBalance.setSeed(seedMoney); // 시작 자본 설정

            // GameBalance 객체를 데이터베이스 저장
            gameBalance = gameBalanceRepository.save(gameBalance);

            // 생성된 GameBalance 리스트에 추가
            gameBalances.add(gameBalance);
        }
        return gameBalances;
    }


    @Override
    @Transactional
    public GameBalanceDTO getGameBalance(Long userId) {
        GameBalance gameBalance = gameBalanceRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(ExceptionEnum.BALANCE_NOT_FOUND));

        Long initialBalance = gameBalance.getSeed();

        List<TransactionsResponse> transactions = transactionRepository.findAllByBalancesIdWithCoinSymbol(gameBalance.getGameBalancesId());

        Long currentBalance = calculateCurrentBalance(initialBalance, transactions);
        Long orderAmount = calculateTotalOrderAmount(transactions);
        Long profitAmount = Math.max(currentBalance - initialBalance, 0L);
        Long lossAmount = Math.max(initialBalance - currentBalance, 0L);

        String lastSymbol = transactions.isEmpty() ? "" : transactions.get(transactions.size() - 1).getSymbol();
        Long lastPrice = transactions.isEmpty() ? 0L : transactions.get(transactions.size() - 1).getPrice();

        GameBalanceDTO gameBalanceDTO = new GameBalanceDTO();
        gameBalanceDTO.setInitialBalance(initialBalance);
        gameBalanceDTO.setCurrentBalance(currentBalance);
        gameBalanceDTO.setOrderAmount(orderAmount);
        gameBalanceDTO.setProfitAmount(profitAmount);
        gameBalanceDTO.setLossAmount(lossAmount);
        gameBalanceDTO.setSymbol(lastSymbol);
        gameBalanceDTO.setPrice(lastPrice);

        return gameBalanceDTO;
    }

    private Long calculateCurrentBalance(Long initialBalance, List<TransactionsResponse> transactions) {
        Long balance = initialBalance;
        for (TransactionsResponse transaction : transactions) {
            if (transaction.getTransactionType() == TransactionType.BUY) {
                balance -= transaction.getTotalPrice();
            } else if (transaction.getTransactionType() == TransactionType.SELL) {
                balance += transaction.getTotalPrice();
            }
        }
        return balance;
    }

    private Long calculateTotalOrderAmount(List<TransactionsResponse> transactions) {
        return transactions.stream()
                .mapToLong(TransactionsResponse::getTotalPrice)
                .sum();
    }

}

