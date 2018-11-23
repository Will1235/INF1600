.data
        factor: .float 2.0 /* use this to multiply by two */

.text
.globl _ZNK9CTriangle7AreaAsmEv

_ZNK9CTriangle7AreaAsmEv:
        push %ebp      /* save old base pointer */
        mov %esp, %ebp /* set ebp to current esp */
        
        sub $4, %esp /* save space for local variable p */

        mov 8(%ebp), %eax /* access to this (pointer) */
	mov 0(%eax), %ecx /* ecx contains virtual table pointer */

	push %eax /* stacking "this" pointer */
	call *12(%ecx) /* calling CTriangle::PerimeterAsm from vtable */
	add $4, %esp /* clean parameters */

	/* st[0] contains the perimeter */

	fld factor /* push 2.0f to float stack */
	fdivrp /* st[0] = perimeter / 2.0f */
	fstp -4(%ebp) /* store p to local variable */
	
	fld -4(%ebp) /* load p to float stack */
	fld 4(%eax) /* load mSides[0] to float stack */
	fsubrp	/* st[0] = p - mSides[0] */

	fld -4(%ebp) /* load p to float stack */
	fld 8(%eax) /* load mSides[1] to float stack */
	fsubrp	/* st[0] = p - mSides[1] */

	fld -4(%ebp) /* load p to float stack */
	fld 12(%eax) /* load mSides[2] to float stack */
	fsubrp	/* st[0] = p - mSides[2] */

	fmulp /* st[0] = (p - mSides[1])*(p - mSides[2]) */
	fmulp /* st[0] = result*(p - mSides[0]) */

	fld -4(%ebp) /* load p to float stack */
	fmulp /* st[0] = result*p */
	fsqrt /* st[0] = sqrt(result) */
        
        leave          /* restore ebp and esp */
        ret            /* return to the caller */

