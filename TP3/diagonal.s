.globl matrix_diagonal_asm

matrix_diagonal_asm:
        push %ebp      /* save old base pointer */
        mov %esp, %ebp /* set ebp to current esp */
    
       #saving live registers
        push %esi 
        push %edi 
        push %ebx 

        #moving matorder into ebx
        movl 16(%ebp),%ebx
        #assigning 0 to r found in %ecx
        movl $0, %ecx

.L1 : 
        #compare r and matorder
        cmp %ebx, %ecx
        #if matorder >= r jump to the break
        jge .BREAK1

        #if the condition is respected continue
        #assigning 0 to c found in %edi 
        movl $0, %edi

.L2 : 
        #compare c and matorder
        cmp %ebx, %edi
        #if matorder >= c jump to the break
        jge .BREAK2

        #assigning eax at the beginning of loop 2 to not make a repetition in 2 conditions
        #place ecx(r) in eax
        #moving into a new register because ecx is saved for incrementation
        mov %ecx, %eax
		#multiply eax with ebx(contains matorder)
        mul %ebx 
		#add edi into eax(add the content of edi and eax)
        #edi + eax = c+(r*matorder)
        addl %edi, %eax 

.EQUAL:
        #if the condition is respected continue
        #compare ecx and edi : (edi - ecx)
        cmp %ecx, %edi
        #if comparison is not equal to 0 (in other words is equal)jump
        jne .NOTEQUAL

        #using esi to store the adress of inmatdata
        movl 8(%ebp), %esi 
        #move inmatdata[c + r * matorder] into esi
        movl (%esi,%eax,4), %esi 
        #using edx to store the adress of outmatdata
        movl 12(%ebp), %edx 
        #move inmatdata[c + r * matorder] into outmatdta
        movl %esi, (%edx,%eax,4) 
        jmp .ENDEQUAL

.NOTEQUAL:		
        #using edx to store the adress of outmatdata
		movl 12(%ebp), %edx 
        #move inmatdata[c + r * matorder] into outmatdta
		movl $0, (%edx,%eax,4) 

.ENDEQUAL :
        #increment c
		incl %edi #edi++ 
        #start second loop again
		jmp .L2

.BREAK2:
        #increment r
		incl %ecx /* ecx++ */
        #start first loop again
		jmp .L1

.BREAK1:

END:
        pop %ebx 
        pop %edi
        pop %esi

    leave          			/* Restore ebp and esp */
    ret            			/* Return to the caller */