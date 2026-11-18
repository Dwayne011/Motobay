package uk.motobay.app.data

/** How to pay at a bay: the cashless provider and the location code on the sign. */
data class Payment(val provider: String, val locationCode: String)

private val PAY = Regex("""Pay by RingGo, location (\d{3,8})""")

/** RingGo and the location code from the council's charging rules, when they give them. */
val Bay.payment: Payment?
    get() = rules?.let { PAY.find(it) }?.let { Payment("RingGo", it.groupValues[1]) }
