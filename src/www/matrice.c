#include <stdio.h>
#include <stdlib.h>
float** allocation2D(int t1, int t2);
void displayResult(int dim, float** A, float* b, float* resultat);
float somme(int dim, float** A, float* x, int i);
void triangularisation(int dim, float** A, float* b);
float* triangulaireSup(int dim, float** A, float* b);
int main()
{
	printf("Calcule de x tel que Ax=b par la meth. de Gauss\n");
	 
	/// Entrée des données
	int dim = 3;							// dimension de la matrice
	float* b = malloc(dim*sizeof(float));	// le vecteur b 'second membre'
	float** A = allocation2D(dim, dim);		// la matrice A 
	
	A[0][0] = 4;
	A[0][1] = 8;
	A[0][2] = 12;
	
	A[1][0] = 3;
	A[1][1] = 8;
	A[1][2] = 13;
	
	A[2][0] = 2;
	A[2][1] = 9;
	A[2][2] = 18;
	
	b[0] = 4;
	b[1] = 5;
	b[2] = 11;
	/// Calcul
	triangularisation(dim, A, b);
	float* resultat = triangulaireSup(dim, A, b);
	
	/// Sortie des résultats
	displayResult(dim, A, b, resultat);
	
	return 0;
}

float* triangulaireSup(int dim, float** A, float* b) {
	float x = 0;
	x = b[dim-1] / A[dim-1][dim-1];
	float* result = malloc(dim*sizeof(float));
	result[dim-1] = x;
	float s = 0;
	for(int i = (dim-2); i >= 0 ; i--) {
		s = somme(dim, A, result, i);
		x = (1/A[i][i]) * (b[i]-s);
		result[i] = x;
	}
	
	return result;
}

void triangularisation(int dim, float** A, float* b) {
	for(int k=0; k <= dim - 1; k++) {
		for(int i=k+1; i <= dim - 1; i++){
			for(int j=k+1; j <= dim - 1; j++){
				A[i][j] = A[i][j] - ((A[i][k]/A[k][k]) * A[k][j]);
			}
			b[i] = b[i] - ((A[i][k]/A[k][k]) * b[k]);
			
			A[i][k] = 0;
		}
	}
}

float somme(int dim, float** A, float* x, int i){
	float sum = 0;
	for(int j = i+1 ; j <= dim-1; j++) {
		sum += A[i][j]*x[j];
	}
	
	return sum;
}

void displayResult(int dim, float** A, float* b, float* resultat) {
	for (int i = 0; i < dim; i++) {
        for (int j = 0; j < dim; j++) {
            printf("\t%g", A[i][j]); 
        }
        printf("\n"); 
    }
    
    printf("Le resultat de la resolution est : ");
    for(int i = 0; i < dim; i++){
		printf("x%i = %g, ", i,resultat[i]);
	}
}

float** allocation2D(int t1, int t2){
	float** A = malloc(t1*sizeof(float*));	
	for(int i = 0; i < t1;  i++){
		A[i]=malloc(t2*sizeof(float));
	}
	
	return A;
}
