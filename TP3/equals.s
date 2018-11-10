.globl matrix_equals_asm

matrix_equals_asm:
        push %ebp                        /* Save old base pointer */
        mov %esp, %ebp                  /* Set ebp to current esp */

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

        #if the condition is respected continue
        #place ecx(r) in eax
        #moving into a new register because ecx is saved for incrementation
        mov %ecx, %eax
        #multiply eax with ebx(contains matorder)
        mul %ebx
        #add edi into eax(add the content of edi and eax)
        #edi + eax = c+(r*matorder)
        addl %edi, %eax
        #using esi to store the adress of inmatdata1
        movl 8(%ebp), %esi
        #move inmatdata1[c + r * matorder] into esi
        movl (%esi,%eax,4), %esi 
        #using edx to store the adress of inmatdata2
        movl 12(%ebp), %edx 
        #move inmatdata2[c + r * matorder] into edx
        movl (%edx,%eax,4), %edx 
        
        #compare edx and esi : (esi - edx)
        cmp %edx, %esi 
        #if comparison is equal to 0 (in other words is equal), jump
        je .EQUALITY 

        #if comparison are not equal 
        #return 0 (eax)
        movl $0, %eax 
        #go to the end of code 
        jmp END 

.EQUALITY : 
        #getting out of second loop and increment c
        incl %edi #edi++
        #start second loop again 
        jmp .L2

.BREAK2 : 
        #not necessary to return 1 in this break
        #it is going to return in break1
        #increment r 
        incl %ecx #ecx++
        #jump back to first loop 
        jmp .L1
   
.BREAK1 : 
        #eax, register for return
        #return 1 
        movl $1, %eax 
END :
        pop %ebx
        pop %edi
        pop %esi 

        #restore ebp and esp
        leave  
        #return to the caller 
        ret