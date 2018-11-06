.data
	i:
		.int 0

.global func_s

func_s:	
	boucle:
		mov d, %eax
		add e, %eax
		sub b, %eax
		mov %eax, a
		if:
			mov b, %eax
			sub $1000, %eax
			mov c, %ebx
			add $500, %ebx
			cmp %ebx, %eax
			jge else
			subl $500, c
			if2:
				mov c, %eax
				cmp b, %eax
				jge endif2
				subl $500, b
			endif2:
			jmp endif
		else:
			mov e, %eax
			sub %eax, b
			addl $500, d
		endif:
	continue:
		addl $1, i
		cmpl $10, i
		jng boucle
	break:
		ret
