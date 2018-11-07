.globl matrix_equals_asm

matrix_equals_asm:
        push %ebp                        /* Save old base pointer */
        mov %esp, %ebp                   /* Set ebp to current esp */

        /* Write your solution here */
        push %esi
        push %ecx
        push %edx
        push %ebx
        push %edi
        movl 16(%ebp), %esi             /* matorder*/
        movl $0, %ebx                   /* c = 0 */
boucle1:
        cmpl %ebx, %esi                 /* comparer matorder a 0 */
        jge arret_boucle1
        movl $0, %edi                   /* r = 0 */
boucle2:
        cmpl %edi, %esi                 /* comparer matorder a 0 */
        jge arret_boucle2
        mov %ebx, %eax                  
        imul %esi, %eax                 /* r * matorder a placer dans eax */
        addl %ebx, %eax                 /* addition c + ( r * matorder ) */
        movl 8(%ebp), %edx              /*inmatdata1*/
        movl (%edx, %eax, 4), %edx      
        movl 12(%ebp), %ecx             /* inmatdata2*/
        movl (%ecx, %eax, 4), %ecx
        cmp %ecx, %edx
        je if_equal
        mov $0, %eax
        jmp end

if_equal:
         incl %edi                      /* ++c*/
         jmp boucle2

arret_boucle2:
        incl %ebx                       /* ++r*/
        jmp boucle1

arret_boucle1:
        movl $1, %eax                   /* retourner 1*/

end:
        pop %ebx
        pop %edi
        pop %esi
        pop %edx
        pop %ecx

        leave          /* Restore ebp and esp */
        ret            /* Return to the caller */
        