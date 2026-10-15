package mn.demo.mini_bank.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;

@Service
public class CurrencyConversionService {

    private static final BigDecimal USD_RATE =
            new BigDecimal("3600.00");

    private static final BigDecimal MAX_AMOUNT =
            new BigDecimal("99999999999999999.99");

    public ConversionResult convert(
            BigDecimal amount,
            String currency) {

        if (amount == null ||
                amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Amount must be greater than zero");
        }

        BigDecimal rate;

        if ("MNT".equals(currency)) {

            if (amount.stripTrailingZeros().scale() > 0) {
                throw new IllegalArgumentException(
                        "MNT amount must be a whole number");
            }

            rate = BigDecimal.ONE;

        } else if ("USD".equals(currency)) {

            if (amount.scale() > 2) {
                throw new IllegalArgumentException(
                        "USD amount supports up to 2 decimal places");
            }

            rate = USD_RATE;

        } else {
            throw new IllegalArgumentException(
                    "Only MNT and USD are supported");
        }

        // Check requested amount against database precision.
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            throw new IllegalArgumentException(
                    "Amount exceeds the allowed limit");
        }

        BigDecimal settlementAmount = amount
                .multiply(rate)
                .setScale(2, RoundingMode.UNNECESSARY);

        // Conversion can exceed database precision even when the input fits.
        if (settlementAmount.compareTo(MAX_AMOUNT) > 0) {
            throw new IllegalArgumentException(
                    "Converted amount exceeds the allowed limit");
        }

        return new ConversionResult(
                amount.setScale(2, RoundingMode.UNNECESSARY),
                currency,
                rate,
                settlementAmount,
                "MNT"
        );
    }

    public record ConversionResult(
            BigDecimal requestedAmount,
            String requestedCurrency,
            BigDecimal exchangeRate,
            BigDecimal settlementAmount,
            String settlementCurrency
    ) {}
}
