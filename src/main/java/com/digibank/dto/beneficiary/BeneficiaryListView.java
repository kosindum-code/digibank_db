package com.digibank.dto.beneficiary;

import com.digibank.enums.BeneficiaryAccountType;
import com.digibank.enums.BeneficiaryStatus;
import com.digibank.enums.BeneficiaryType;
import com.digibank.enums.BeneficiaryVerificationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BeneficiaryListView {

	private final Long id;
	private final String beneficiaryName;
	private final String nickname;
	private final String bankName;
	private final String maskedAccountNumber;
	private final BeneficiaryAccountType accountType;
	private final BeneficiaryType beneficiaryType;
	private final BeneficiaryStatus status;
	private final boolean favourite;
	private final BigDecimal transferLimit;
	private final BeneficiaryVerificationStatus verificationStatus;
	private final LocalDateTime createdAt;

	public BeneficiaryListView(Long id, String beneficiaryName, String nickname, String bankName,
			String maskedAccountNumber, BeneficiaryAccountType accountType, BeneficiaryType beneficiaryType,
			BeneficiaryStatus status, boolean favourite, LocalDateTime createdAt) {
		this(id, beneficiaryName, nickname, bankName, maskedAccountNumber, accountType, beneficiaryType, status,
				favourite, null, null, createdAt);
	}

	public BeneficiaryListView(Long id, String beneficiaryName, String nickname, String bankName,
			String maskedAccountNumber, BeneficiaryAccountType accountType, BeneficiaryType beneficiaryType,
			BeneficiaryStatus status, boolean favourite, BigDecimal transferLimit,
			BeneficiaryVerificationStatus verificationStatus, LocalDateTime createdAt) {
		this.id = id;
		this.beneficiaryName = beneficiaryName;
		this.nickname = nickname;
		this.bankName = bankName;
		this.maskedAccountNumber = maskedAccountNumber;
		this.accountType = accountType;
		this.beneficiaryType = beneficiaryType;
		this.status = status;
		this.favourite = favourite;
		this.transferLimit = transferLimit;
		this.verificationStatus = verificationStatus;
		this.createdAt = createdAt;
	}

	public Long getId() {
		return id;
	}

	public String getBeneficiaryName() {
		return beneficiaryName;
	}

	public String getNickname() {
		return nickname;
	}

	public String getBankName() {
		return bankName;
	}

	public String getMaskedAccountNumber() {
		return maskedAccountNumber;
	}

	public BeneficiaryAccountType getAccountType() {
		return accountType;
	}

	public BeneficiaryType getBeneficiaryType() {
		return beneficiaryType;
	}

	public BeneficiaryStatus getStatus() {
		return status;
	}

	public boolean isFavourite() {
		return favourite;
	}

	public BigDecimal getTransferLimit() {
		return transferLimit;
	}

	public BeneficiaryVerificationStatus getVerificationStatus() {
		return verificationStatus;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
