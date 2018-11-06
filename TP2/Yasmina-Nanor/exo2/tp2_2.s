.global func_s

func_s:
	flds b 		#mettre b sur la pile à la position s[0]
	flds d 		#mettre d sur la pile à la position s[0] et b prend la position s[1]
	fmulp 		#b * d (position s[0])
	flds c 		#mettre c sur la pile à la position s[0] alors la multiplication devient s[1]
	fsubrp 		#(b * d) - c (position s[0]) 
	flds g 		#mettre g sur la pile à la position s[0] et la soustraction devient s[1]
	flds f 		#f sur la pile à la position s[0], la soustraction s[1] et la multiplication devient s[2]
	fsubp 		#f - g (position s[0]) et la ligne 8 devient devient s[1]
	fdivrp 		#((b * d) - c) / (f-g) (position s[0])
	flds e 		#e sur la pile à la position s[0] et la ligne précédente devient s[1]
	faddp 		#e + (((b * d) - c) / (f-g)) (position s[0])
	flds e 		#mettre e sur la pile à la position s[0] et la ligne précédente devient s[1]	
	flds g 		#g sur la pile à la position s[0], e prend la position s[1] et la ligne 14 devient s[2]	
	fsubp 		#g - e (position s[0]) et la ligne 14 devient devient s[1]	
	flds f 		#f sur la pile à la position s[0], la soustraction devient s[1] et la ligne 14 devient s[2]	
	fdivrp 		#(g-e) / f (position s[0]) et la ligne 14 devient s[1]	
	fmulp 		#(e + (((b * d) - c) / (f-g))) / ((g-e) / f) (position s[0])	
	fstps a 	#pousser le sommet de la pile à l'adresse dans la mémoire

	ret #retouner resultat
