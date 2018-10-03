package com.giyeok.passzero2.util

import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.Single
import io.reactivex.SingleEmitter

fun <U, T> Single<U>.mapPropagatingError(func: (U) -> T): Single<T> =
        Single.create { emitter: SingleEmitter<T> ->
            this.subscribe({ v -> emitter.onSuccess(func(v)) }, { error -> emitter.onError(error) })
        }

fun <U, T> Observable<U>.mapPropagatingError(func: (U) -> T): Observable<T> =
        Observable.create { emitter: ObservableEmitter<T> ->
            this.subscribe(
                    { v -> emitter.onNext(func(v)) },
                    { error -> emitter.onError(error) },
                    { emitter.onComplete() })
        }
