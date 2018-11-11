.global matrix_row_aver_asm

matrix_row_aver_asm:
        push %ebp      			/* Save old base pointer */
        mov %esp, %ebp 			/* Set ebp to current esp */

		#saving live registers
        push %esi 
        push %edi
        push %ebx

        #making room for 2 integers(r, c)
        add $8, %esp
        #putting 0 into r 
        movl $0, -16(%ebp) 
        #moving matorder into ebx
        movl 16(%ebp), %ebx  
L1:
        #comparing value of r and matorder
        cmp %ebx, -16(%ebp) 
        #if matorder >= r jump to the break
        jge BREAK1 

        #assigning elem to edi and putting it to 0
        mov $0, %edi

        #putting 0 into c
        movl $0, -20(%ebp) 
L2:
        #comparing value of c and matorder
        cmp %ebx, -20(%ebp)
        #if matorder >= c jump to the break
        jge BREAK2 

        #if the condition is respected continue
        #moving the value of r into eax 
        mov -16(%ebp), %eax
        #multiply eax with ebx(contains matorder)
        mul %ebx 
        #add c to eax (r*matorder)
        addl -20(%ebp), %eax 

        #using ecx to store adress of inmatdata
        movl 8(%ebp), %ecx 
        #inmatdata[c + r * matorder]into ecx
        movl (%ecx,%eax,4), %ecx 

        #elem is edi and adding eax (elem+=)
        addl %ecx, %edi 

        #incrementer c
        incl -20(%ebp)
        jmp L2

BREAK2:

BREAK1: 

		
        leave          			/* Restore ebp and esp */
        ret           			/* Return to the caller */