package ma.projet.restclient

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import ma.projet.restclient.R
import ma.projet.restclient.adapter.CompteAdapter
import ma.projet.restclient.adapter.CompteAdapter.OnDeleteClickListener
import ma.projet.restclient.adapter.CompteAdapter.OnUpdateClickListener
import ma.projet.restclient.entities.Compte
import ma.projet.restclient.repository.CompteRepository
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), OnDeleteClickListener, OnUpdateClickListener {
    private var recyclerView: RecyclerView? = null
    private var adapter: CompteAdapter? = null
    private var formatGroup: RadioGroup? = null
    private var addbtn: FloatingActionButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_main)

            initViews()
            setupRecyclerView()
            setupFormatSelection()
            setupAddButton()

            loadData("JSON")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Erreur: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun initViews() {
        recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        formatGroup = findViewById<RadioGroup>(R.id.formatGroup)
        addbtn = findViewById<FloatingActionButton>(R.id.fabAdd)
    }

    private fun setupRecyclerView() {
        recyclerView?.let {
            it.layoutManager = LinearLayoutManager(this)
            adapter = CompteAdapter(this, this)
            it.adapter = adapter
        }
    }

    private fun setupFormatSelection() {
        formatGroup?.setOnCheckedChangeListener { group: RadioGroup?, checkedId: Int ->
            if (checkedId != -1) { // Évite le déclenchement lors de l'initialisation
                val format = if (checkedId == R.id.radioJson) "JSON" else "XML"
                loadData(format)
            }
        }
    }

    private fun setupAddButton() {
        addbtn?.setOnClickListener { showAddCompteDialog() }
    }

    private fun showAddCompteDialog() {
        val builder = AlertDialog.Builder(this@MainActivity)
        val dialogView: View = getLayoutInflater().inflate(R.layout.dialog_add_compte, null)

        val etSolde = dialogView.findViewById<EditText>(R.id.etSolde)
        val typeGroup = dialogView.findViewById<RadioGroup>(R.id.typeGroup)

        builder.setView(dialogView)
            .setTitle("Ajouter un compte")
            .setPositiveButton("Ajouter", DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                val soldeStr = etSolde?.text?.toString() ?: ""
                if (soldeStr.isBlank()) {
                    showToast("Veuillez entrer un solde")
                    return@OnClickListener
                }
                
                try {
                    val solde = soldeStr.toDouble()
                    val type = if (typeGroup?.checkedRadioButtonId == R.id.radioCourant)
                        "COURANT"
                    else
                        "EPARGNE"

                    val formattedDate = this.currentDateFormatted
                    val compte = Compte(null, solde, type, formattedDate)
                    addCompte(compte)
                } catch (e: NumberFormatException) {
                    showToast("Le solde doit être un nombre valide")
                }
            })
            .setNegativeButton("Annuler", null)

        val dialog = builder.create()
        dialog.show()
    }

    private val currentDateFormatted: String
        get() {
            val calendar = Calendar.getInstance()
            val formatter = SimpleDateFormat("yyyy-MM-dd")
            return formatter.format(calendar.getTime())
        }

    private fun addCompte(compte: Compte?) {
        if (compte == null) return
        val compteRepository = CompteRepository("JSON")
        compteRepository.addCompte(compte, object : Callback<Compte> {
            override fun onResponse(call: Call<Compte>, response: Response<Compte>) {
                if (response.isSuccessful()) {
                    showToast("Compte ajouté")
                    loadData("JSON")
                } else {
                    val errorMsg = "Erreur: ${response.code()} - ${response.message()}"
                    showToast(errorMsg)
                }
            }

            override fun onFailure(call: Call<Compte>, t: Throwable) {
                val errorMsg = when {
                    t.message?.contains("Failed to connect") == true -> 
                        "Serveur non accessible. Vérifiez que le serveur est démarré sur http://10.0.2.2:8080"
                    t.message?.contains("timeout") == true -> 
                        "Timeout: Le serveur ne répond pas"
                    else -> "Erreur: ${t.message ?: "Connexion impossible"}"
                }
                showToast(errorMsg)
                t.printStackTrace()
            }
        })
    }

    private fun loadData(format: String?) {
        try {
            val compteRepository = CompteRepository(format ?: "JSON")
            compteRepository.getAllCompte(object : Callback<List<Compte>> {
                override fun onResponse(call: Call<List<Compte>>, response: Response<List<Compte>>) {
                    if (response.isSuccessful() && response.body() != null) {
                        val comptes: List<Compte> = response.body()!!
                        runOnUiThread {
                            if (comptes.isEmpty()) {
                                showToast("Aucune donnée disponible")
                            } else {
                                adapter?.updateData(comptes)
                            }
                        }
                    } else {
                        runOnUiThread {
                            val errorMsg = "Erreur ${response.code()}: ${response.message()}"
                            showToast(errorMsg)
                            // Log pour debug
                            android.util.Log.e("MainActivity", "Response error: ${response.code()} - ${response.message()}")
                            if (response.errorBody() != null) {
                                try {
                                    val errorString = response.errorBody()?.string()
                                    android.util.Log.e("MainActivity", "Error body: $errorString")
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<List<Compte>>, t: Throwable) {
                    runOnUiThread {
                        val errorMsg = when {
                            t.message?.contains("Failed to connect") == true -> 
                                "Serveur non accessible. Vérifiez que le serveur est démarré sur http://10.0.2.2:8080"
                            t.message?.contains("timeout") == true -> 
                                "Timeout: Le serveur ne répond pas"
                            else -> "Erreur: ${t.message ?: "Connexion impossible"}"
                        }
                        showToast(errorMsg)
                    }
                    t.printStackTrace()
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
            showToast("Erreur lors du chargement: ${e.message}")
        }
    }

    override fun onUpdateClick(compte: Compte) {
        showUpdateCompteDialog(compte)
    }

    private fun showUpdateCompteDialog(compte: Compte) {
        val builder = AlertDialog.Builder(this@MainActivity)
        val dialogView: View = getLayoutInflater().inflate(R.layout.dialog_add_compte, null)

        val etSolde = dialogView.findViewById<EditText>(R.id.etSolde)
        val typeGroup = dialogView.findViewById<RadioGroup>(R.id.typeGroup)
        etSolde?.setText(compte.getSolde().toString())
        if (compte.getType().equals("COURANT", ignoreCase = true)) {
            typeGroup?.check(R.id.radioCourant)
        } else if (compte.getType().equals("EPARGNE", ignoreCase = true)) {
            typeGroup?.check(R.id.radioEpargne)
        }

        builder.setView(dialogView)
            .setTitle("Modifier un compte")
            .setPositiveButton("Modifier", DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                val solde = etSolde?.text?.toString() ?: ""
                val type = if (typeGroup?.checkedRadioButtonId == R.id.radioCourant)
                    "COURANT"
                else
                    "EPARGNE"
                compte.setSolde(solde.toDouble())
                compte.setType(type)
                updateCompte(compte)
            })
            .setNegativeButton("Annuler", null)

        val dialog = builder.create()
        dialog.show()
    }

    private fun updateCompte(compte: Compte) {
        val compteRepository = CompteRepository("JSON")
        compteRepository.updateCompte(compte.getId(), compte, object : Callback<Compte> {
            override fun onResponse(call: Call<Compte>, response: Response<Compte>) {
                if (response.isSuccessful()) {
                    showToast("Compte modifié")
                    loadData("JSON")
                }
            }

            override fun onFailure(call: Call<Compte>, t: Throwable) {
                showToast("Erreur lors de la modification")
            }
        })
    }

    override fun onDeleteClick(compte: Compte) {
        showDeleteConfirmationDialog(compte)
    }

    private fun showDeleteConfirmationDialog(compte: Compte) {
        AlertDialog.Builder(this)
            .setTitle("Confirmation")
            .setMessage("Voulez-vous vraiment supprimer ce compte ?")
            .setPositiveButton(
                "Oui",
                DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int -> deleteCompte(compte) })
            .setNegativeButton("Non", null)
            .show()
    }

    private fun deleteCompte(compte: Compte) {
        val compteRepository = CompteRepository("JSON")
        compteRepository.deleteCompte(compte.getId(), object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful()) {
                    showToast("Compte supprimé")
                    loadData("JSON")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                showToast("Erreur lors de la suppression")
            }
        })
    }

    private fun showToast(message: String?) {
        runOnUiThread {
            Toast.makeText(this@MainActivity, message ?: "Erreur inconnue", Toast.LENGTH_LONG).show()
        }
    }
}