.global matrix_row_aver_asm

matrix_row_aver_asm:
        push %ebp      					/* Save old base pointer */
        mov %esp, %ebp 					/* Set ebp to current esp */

		/* Write your solution here */
		push %esi
        push %ebx
        push %edi
        movl 16(%ebp), %esi             /* matorder*/
        movl $0, %ebx                   /* r = 0 */
boucle1:
        cmp %esi, %ebx                 /* comparer matorder a r */
        jge end
		movl $0, %edi                   /* elem = 0 */
        movl $0, %ecx					/* c = 0 */
        
boucle2:
        cmp %esi, %ecx           		 /* comparer matorder a c */
        jge arret_boucle2
        mov %ebx, %eax                  /* r dans eax */
        imul %esi, %eax                 /* r * matorder a placer dans eax */
        addl %ecx, %eax           		/* addition c + ( r * matorder ) */
        movl 8(%ebp), %edx              /* inmatdata = edx */     
        addl (%edx, %eax, 4), %edi		/* elem += inmatdata[c + r * matorder] */
        incl %ecx						/* ++c */
        jmp boucle2
        
arret_boucle2:
		movl $0, %edx
		movl %edi, %eax           		/* met elem dans eax */
        div %esi						/* elem/matorder */
        movl 12(%ebp), %edx				/* outmatdata dans edx */
        movl %eax, (%edx, %ebx, 4)		/* outmatdata [r] = elem/matorder */
        incl %ebx                       /* ++r */
        jmp boucle1
end:      
        pop %edi
        pop %ebx
        pop %esi
        
        leave          					/* Restore ebp and esp */
        ret           					/* Return to the caller */
        
