package fr.jeff.perf.ui

object Routes {
    const val EXERCICES = "exercices"
    const val JOURNAL = "journal"
    const val POIDS = "poids"
    const val GESTION = "gestion"
    const val FICHE = "fiche"
    const val SAISIE = "saisie"

    fun fiche(exId: Long) = "$FICHE/$exId"
    fun nouvelleSaisie(exId: Long? = null) = if (exId != null) "$SAISIE?exId=$exId" else SAISIE
    fun modifierSaisie(perfId: Long) = "$SAISIE?perfId=$perfId"
}
