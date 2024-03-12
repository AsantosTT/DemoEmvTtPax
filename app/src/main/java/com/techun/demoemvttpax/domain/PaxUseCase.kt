package com.techun.demoemvttpax.domain

import com.techun.demoemvttpax.domain.repository.PaxRepository
import com.techun.demoemvttpax.utils.DataState
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class PaxUseCase @Inject constructor(private val paxRepository: PaxRepository) {
    suspend operator fun invoke(): Flow<DataState<Boolean>> = paxRepository.initSdk()
}