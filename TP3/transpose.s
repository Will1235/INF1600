.globl matrix_transpose_asm

matrix_transpose_asm:
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

        #if the condition is respected continue
        #place edi(c) in eax
        #moving into a new register because edi is saved for incrementation
        mov %edi, %eax
        #multiply eax with ebx(contains matorder)
        mul %ebx
        #add ecx into eax(add the content of ecx and eax)
        #ecx + eax = r+(c*matorder)
        addl %ecx, %eax
        #using esi to store the adress of inmatdata
        movl 8(%ebp), %esi
        #move inmatdata[c + r * matorder] into esi
        movl (%esi,%eax,4), %esi 
        #put ecx(r) into eax 
        mov %ecx, %eax
        #multiply eax with ebx(contains matorder)
	mul %ebx 
        #edi + eax = c+(r*matorder)
	addl %edi, %eax 
        #using edx to store the adress of outmatdata
        movl 12(%ebp), %edx 
        #move inmatdata[c + r * matorder] into outmatdata
        movl %esi, (%edx,%eax,4)
        
        #increment c  
        incl %edi #edi++
        #start second loop again 
        jmp .L2 
       
.BREAK2 : 
        #increment r 
        incl %ecx #ecx++
        #jump back to first loop 
        jmp .L1
.BREAK1: 

END :
        pop %ebx
        pop %edi
        pop %esi 

        #restore ebp and esp
        leave  
        #return to the caller 
        ret