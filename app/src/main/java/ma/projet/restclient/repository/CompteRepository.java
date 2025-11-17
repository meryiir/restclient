package ma.projet.restclient.repository;

import ma.projet.restclient.api.CompteService;
import ma.projet.restclient.entities.Compte;
import ma.projet.restclient.entities.CompteList;
import ma.projet.restclient.config.RetrofitClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
public class CompteRepository {
    private CompteService compteService;
    private String format;

    // Constructeur pour initialiser le service avec le type de convertisseur
    public CompteRepository(String converterType) {
        compteService = RetrofitClient.getClient(converterType).create(CompteService.class);
        this.format = converterType;
    }

    // Méthode pour récupérer tous les comptes
    public void getAllCompte(Callback<List<Compte>> callback) {
        if ("JSON".equals(format)) {
            Call<List<Compte>> call = compteService.getAllCompteJson();
            call.enqueue(callback);
        } else {
            Call<CompteList> call = compteService.getAllCompteXml();
            final Call<List<Compte>> dummyCall = compteService.getAllCompteJson();
            call.enqueue(new Callback<CompteList>() {
                @Override
                public void onResponse(Call<CompteList> call, Response<CompteList> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        // Convertir CompteList en List<Compte>
                        List<Compte> comptes = response.body().getComptes();
                        Response<List<Compte>> successResponse = Response.success(comptes);
                        callback.onResponse(dummyCall, successResponse);
                    } else {
                        callback.onFailure(dummyCall, new Throwable("Response not successful"));
                    }
                }

                @Override
                public void onFailure(Call<CompteList> call, Throwable t) {
                    callback.onFailure(dummyCall, t);
                }
            });
        }
    }

    // Méthode pour récupérer un compte par son ID
    public void getCompteById(Long id, Callback<Compte> callback) {
        Call<Compte> call = compteService.getCompteById(id);
        call.enqueue(callback);
    }

    // Méthode pour ajouter un compte
    public void addCompte(Compte compte, Callback<Compte> callback) {
        Call<Compte> call = compteService.addCompte(compte);
        call.enqueue(callback);
    }

    // Méthode pour mettre à jour un compte
    public void updateCompte(Long id, Compte compte, Callback<Compte> callback) {
        Call<Compte> call = compteService.updateCompte(id, compte);
        call.enqueue(callback);
    }

    // Méthode pour supprimer un compte
    public void deleteCompte(Long id, Callback<Void> callback) {
        Call<Void> call = compteService.deleteCompte(id);
        call.enqueue(callback);
    }
}
