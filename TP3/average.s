.global matrix_multiply_asm

matrix_multiply_asm:
        push %ebp      /* save old base pointer */
        mov %esp, %ebp /* set ebp to current esp */
       
        #saving live registers
        push %esi 
        push %edi
        push %ebx

        #making room for 3 integers(r, c and i)
        add $12, %esp 
        #putting 0 into r 
        #not assigning a register(we will need them) 
        movl $0, -16(%ebp) 
        #moving matorder into ebx
        movl 20(%ebp), %ebx 

L1:
        #comparing value of r and matorder
        cmp %ebx, -16(%ebp) 
        #if matorder >= r jump to the break
        jge BREAK1 

        #putting 0 into c
        movl $0, -20(%ebp) 

L2:
        #comparing value of c and matorder
        cmp %ebx, -20(%ebp)
        #if matorder >= c jump to the break
        jge BREAK2 

        #assigning elem to edi and putting it to 0
        mov $0, %edi
        #putting 0 into i
        movl $0, -24(%ebp) 

L3:
        #comparing value of i and matorder
        cmp %ebx, -24(%ebp)
        #if matorder >= i jump to the break
        jge BREAK3 

        #if the condition is respected continue
        #moving the value of r into eax 
        mov -16(%ebp), %eax
        #multiply eax with ebx(contains matorder)
        mul %ebx 
        #add i to eax (r*matorder)
        addl -24(%ebp), %eax 

        #using ecx to store adress of inmatdata1
        movl 8(%ebp), %ecx 
        #inmatdata1[i + r * matorder]into ecx
        movl (%ecx,%eax,4), %ecx 
        #moving the value of i into eax
        mov -24(%ebp), %eax
        #multiply eax with ebx (matorder)
        mul %ebx 
        #adding c to eax (i*matorder)
        addl -20(%ebp), %eax 

        #using esi to store adress of inmatdata2
        movl 12(%ebp), %esi 
        #inmatdata2[c + i * matorder]into esi
        movl (%esi,%eax,4), %esi 

        #moving ecx(inmatdata1[i + r * matorder])into eax
        mov %ecx, %eax
        #multpilying with esi (inmatdata2[c + i * matorder])to get in eax
        #eax = inmatdata1[i + r * matorder] * inmatdata2[c + i * matorder]
        mul %esi 
        #elem is edi and adding eax (elem+=)
        addl %eax, %edi 

        #incrementing i 
        incl -24(%ebp) #i++
        jmp L3

BREAK3:
        #moving r into eax
        mov -16(%ebp), %eax
        #multpilying ebx(matorder)with r
        mul %ebx
        #adding c into the multiplication
        addl -20(%ebp), %eax 

        #using esi to store adress of outmatdata
        movl 16(%ebp), %esi 
        #moving outmatdata[c + r*matorder]into elem
        movl %edi, (%esi, %eax, 4)
        
        #incrementer c
        incl -20(%ebp)
        jmp L2

BREAK2:
        #incrementer r
        incl -16(%ebp) 
        jmp L1

BREAK1:

END:
        pop %ebx 
        pop %edi 
        pop %esi     

        leave          
        ret