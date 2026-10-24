package com.voaaguia.api;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
public final class Requests {
 private Requests() {}
 public record Login(@NotBlank @Email String email, @NotBlank String password) {}
 public record Register(@NotBlank @Email String email, @NotBlank @Size(min=8,max=72) String password, @NotBlank @Size(max=120) String name, @NotBlank @Size(max=100) String department) {}
 public record Strategy(@NotBlank @Size(max=160) String title, @NotBlank @Size(max=5000) String description, @NotBlank @Size(max=80) String category, @NotBlank @Size(max=120) String campaign, boolean isActive) {}
 public record IdeaInput(@NotBlank @Size(max=160) String title, @NotBlank @Size(max=5000) String description, @NotBlank @Size(max=80) String category, @NotBlank String strategyId) {}
 public record Review(@NotBlank @Pattern(regexp="EM_ANALISE|APROVADA|REJEITADA") String status, @NotBlank @Pattern(regexp="BAIXA|MEDIA|ALTA|CRITICA") String priority, @NotNull @Size(max=2000) String feedback) {}
 public record ProjectInput(@NotBlank @Size(max=160) String title, @NotBlank @Size(max=5000) String description, @NotBlank String ideaId, @NotBlank String strategyId,
 @NotBlank @Pattern(regexp="PLANEJAMENTO|EM_ANDAMENTO|CONCLUIDO|CANCELADO") String status,
 @NotBlank @Pattern(regexp="IDEACAO|VALIDACAO|DESENVOLVIMENTO|PILOTO|IMPLANTACAO|OPERACAO") String stage,
 @NotNull Instant startDate, @NotNull Instant expectedEndDate, Instant actualEndDate,
 @NotNull @DecimalMin("0") BigDecimal investment, @NotNull @DecimalMin("0") BigDecimal expectedReturn,
 @NotNull @DecimalMin("0") BigDecimal actualReturn, @NotNull @DecimalMin("0") BigDecimal costReduction,
 @Min(0) @Max(100) double productivityGain, @Min(0) @Max(100) int progressPercent, @NotNull @Size(max=5000) String notes) {}
 public record AiInput(@NotBlank @Size(max=160) String title, @NotBlank @Size(max=5000) String description) {}
}
