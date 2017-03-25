;
; Thistle Boot ROM
;
	.setcpu		"6502"

.segment	"STARTUP"

.byte "--[[CABE:Thistle:"

.segment "RODATA"

; TSF Component Data
fslist: .byte 10,10,0,"filesystem",0
umlist: .byte 10,5,0,"drive",0
secread: .byte 10,10,0,"readSector",3,1,0
fsopend: .byte 10,4,0,"open",10,12,0,"Thistle/boot",0
fsopenf: .byte 10,4,0,"open",10,7,0,"Thistle",0
fsclose: .byte 10,5,0,"close"
fsread: .byte 10,4,0,"read",14,0,0,0,0,4,0,1,0

; Messages
greeting: .byte "Thistle Boot ROM",10,10
nomem: .byte "No Memory installed"
drives: .byte "Checking drives ...",10
fsmsg: .byte 10,"Checking filesystems ...",10
bootmsg: .byte "Booting ...",10
noboot: .byte "Nothing to boot from",10

hexlookup: .byte "0123456789abcdef"

unkop: .byte "Unknown Opcode",10
unkcmd: .byte "Unknown Command",10

cmdlist:
.asciiz "run"
.word cmd_run
.asciiz ""

.define inval $FF,$FF,$FF

opcodetbl:
.byte "BRK",$00,"ORA",$09,inval,$00,inval,$09,inval,$03,"ORA",$03,"ASL",$03,inval,$03,"PHP",$00,"ORA",$01,"ASL",$00,inval,$01,inval,$02,"ORA",$02,"ASL",$02,inval,$02
.byte "BPL",$0B,"ORA",$0A,inval,$00,inval,$0A,inval,$06,"ORA",$06,"ASL",$06,inval,$06,"CLC",$00,"ORA",$05,inval,$00,inval,$05,inval,$04,"ORA",$04,"ASL",$04,inval,$04
.byte "JSR",$02,"AND",$09,inval,$00,inval,$09,"BIT",$03,"AND",$03,"ROL",$03,inval,$03,"PLP",$00,"AND",$01,"ROL",$00,inval,$01,"BIT",$02,"AND",$02,"ROL",$02,inval,$02
.byte "BMI",$0B,"AND",$0A,inval,$00,inval,$0A,inval,$06,"AND",$06,"ROL",$06,inval,$06,"SEC",$00,"AND",$05,inval,$00,inval,$05,inval,$04,"AND",$04,"ROL",$04,inval,$04
.byte "RTI",$00,"EOR",$09,inval,$00,inval,$09,inval,$03,"EOR",$03,"LSR",$03,inval,$03,"PHA",$00,"EOR",$01,"LSR",$00,inval,$01,"JMP",$02,"EOR",$02,"LSR",$02,inval,$02
.byte "BVC",$0B,"EOR",$0A,inval,$00,inval,$0A,inval,$06,"EOR",$06,"LSR",$06,inval,$06,"CLI",$00,"EOR",$05,inval,$00,inval,$05,inval,$04,"EOR",$04,"LSR",$04,inval,$04
.byte "RTS",$00,"ADC",$09,inval,$00,inval,$09,inval,$03,"ADC",$03,"ROR",$03,inval,$03,"PLA",$00,"ADC",$01,"ROR",$00,inval,$01,"JMP",$08,"ADC",$02,"ROR",$02,inval,$02
.byte "BVS",$0B,"ADC",$0A,inval,$00,inval,$0A,inval,$06,"ADC",$06,"ROR",$06,inval,$06,"SEI",$00,"ADC",$05,inval,$00,inval,$05,inval,$04,"ADC",$04,"ROR",$04,inval,$04
.byte "BCS",$01,"STA",$09,inval,$01,inval,$09,"STY",$03,"STA",$03,"STX",$03,inval,$03,"DEY",$00,inval,$01,"TXA",$00,inval,$01,"STY",$02,"STA",$02,"STX",$02,inval,$02
.byte "BCC",$0B,"STA",$0A,inval,$00,inval,$0A,"STY",$06,"STA",$06,"STX",$07,inval,$07,"TYA",$00,"STA",$05,"TXS",$00,inval,$05,inval,$04,"STA",$04,inval,$05,inval,$05
.byte "LDY",$01,"LDA",$09,"LDX",$01,inval,$09,"LDY",$03,"LDA",$03,"LDX",$03,inval,$03,"TAY",$00,"LDA",$01,"TAX",$00,inval,$01,"LDY",$02,"LDA",$02,"LDX",$02,inval,$02
.byte "BCS",$0B,"LDA",$0A,inval,$00,inval,$0A,"LDY",$06,"LDA",$06,"LDX",$07,inval,$07,"CLV",$00,"LDA",$05,"TSX",$00,inval,$05,"LDY",$04,"LDA",$04,"LDX",$05,inval,$05
.byte "CPY",$01,"CMP",$09,inval,$01,inval,$09,"CPY",$03,"CMP",$03,"DEC",$03,inval,$03,"INY",$00,"CMP",$01,"DEX",$00,inval,$01,"CPY",$02,"CMP",$02,"DEC",$02,inval,$02
.byte "BNE",$0B,"CMP",$0A,inval,$00,inval,$0A,inval,$06,"CMP",$06,"DEC",$06,inval,$06,"CLD",$00,"CMP",$05,inval,$00,inval,$05,inval,$04,"CMP",$04,"DEC",$04,inval,$04
.byte "CPX",$01,"SBC",$09,inval,$01,inval,$09,"CPX",$03,"SBC",$03,"INC",$03,inval,$03,"INX",$00,"SBC",$01,"NOP",$00,inval,$01,"CPX",$02,"SBC",$02,"INC",$02,inval,$02
.byte "BEQ",$0B,"SBC",$0A,inval,$00,inval,$0A,inval,$06,"SBC",$06,"INC",$06,inval,$06,"SED",$00,"SBC",$05,inval,$00,inval,$05,inval,$04,"SBC",$04,"INC",$04,inval,$04

length: .byte 1,2,3,2,3,3,2,2,3,2,2,2

format:
.byte "#$__",$00,$01
.byte "#'_'",$00,$01
.byte "$____",$00,$02
.byte "$__",$00,$03
.byte "$____,X",$00,$04
.byte "$____,Y",$00,$05
.byte "$__,X",$00,$06
.byte "$__,Y",$00,$07
.byte "($____)",$00,$08
.byte "($__,X)",$00,$09
.byte "($__),Y",$00,$0a
.byte "0X__",$00,$0b
.byte "-0X__",$00,$0b
.byte $00,$00

.byte "]]error",$22,"Thistle architecture required",$22,"--"

.segment "STARTUP"

.macro _copy_base_short src, dest, len, mode
	lda #<src
	sta $E041
	lda #>src
	sta $E042
	lda #<dest
	sta $E043
	lda #>dest
	sta $E044
	lda #<len
	sta $E045
	lda #mode
	sta $e040
.endmacro

.macro copys_pp src, dest, len
	_copy_base_short src, dest, len, $00
.endmacro

.macro copys_pu src, dest, len
	_copy_base_short src, dest, len, $01
.endmacro

.macro copys_up src, dest, len
	_copy_base_short src, dest, len, $02
.endmacro

.macro copys_uu src, dest, len
	_copy_base_short src, dest, len, $03
.endmacro

hexprint:
	; Prints a byte to the screen
	; A - byte to print
	; Clobbers: X
	pha
	tax
	tya
	pha
	txa
	lsr
	lsr
	lsr
	lsr
	tay
	lda hexlookup,Y
	sta $E003
	txa
	and #$0F
	tay
	lda hexlookup,Y
	sta $E003
	pla
	tay
	pla
	rts

uuidprint:
	; Prints a UUID to the screen
	; $00, $01 - Address of UUID
	; Clobbers: A, X (hexprint), Y
	ldy #$00
@loop:	lda ($00),Y
	jsr hexprint
	iny
	cpy #$10
	beq @done
	cpy #$04
	beq @dash
	cpy #$06
	beq @dash
	cpy #$08
	beq @dash
	cpy #$0A
	beq @dash
	jmp @loop
@dash:	lda #$2D
	sta $E003
	jmp @loop
@done:	lda #$0a
	sta $E003
	rts

_readlist:
	; Reads component list to $0200
	; $02 - Bytes to skip for component type
	; Clobbers: A, X, Y, $00, $01
	lda #$00
	sta $00
	tay
	lda #$02
	sta $01
@loop1:	lda $E012
	cmp #$00 ; TSF End Tag
	beq @done

	; Read UUID
	ldx #$10
@loop2:	lda $E012
	sta ($00),Y
	iny
	cpy #$00
	bne @skip
	inc $01 ; Increment $01 when Y wraps to 0
@skip:	dex
	cpx #$00
	bne @loop2

	; Drop component name
	ldx $02
@loop3:	lda $E012
	dex
	cpx #$00
	bne @loop3
	jmp @loop1
@done:	lda #$02
	sta $01
	rts

.macro readlist skip
	lda #skip
	sta $02
	jsr _readlist
.endmacro

loaduuid:
	; Load a UUID into the component selector buffer
	; $00, $01 - Address to read from
	; Clobbers: A, X, Y, (uuidprint)
	jsr uuidprint
	ldy $00
	lda #$00
	sta $00
	ldx #$10
	lda #$0b ; UUID Tag
	sta $E012
@loop:	lda ($00),Y ; UUID Byte loop
	sta $E012
	iny
	cpy #$00
	bne @skip
	inc $01
@skip:	dex
	cpx #$00
	bne @loop
	sty $00
	stx $E012 ; End Tag
	stx $E010 ; Map Component
	rts

bootdrive:
	; Checks and boots from a drive
	lda $D000
	cmp #$00
	beq @boot
	rts
@boot:	copys_up bootmsg, $E003, .sizeof(bootmsg)
	lda #$01 ; Setup Copy Engine
	sta $E041
	lda #$D0
	sta $E042
	lda #$00
	sta $E043
	lda #$02
	sta $E044

	lda $D001 ; Discard tag
	lda $D001
	sta $E045
	lda $D001
	sta $E046

	lda #$01 ; Copy
	sta $E040

	lda #$00
	sta $E046
	jmp $0200 ; Boot

bootfs:
	; Boots from a file
	lda $D000
	cmp #$00
	beq @boot
	rts
@boot:	copys_up bootmsg, $E003, .sizeof(bootmsg)
	lda #$02 ; Something to boot!
	sta $00
	copys_uu fsread, $0001, .sizeof(fsread) ; Copy read command
	copys_pu $D001, $0008, 5 ; Inject handle

@loop:	ldx $00
	cpx #$D0
	beq @done ; Too much data read
	copys_up $0001, $D001, .sizeof(fsread) ; Call "read"
	lda #$00
	sta $D000

	lda $D001 ; Check TSF Tag
	cmp #$09 ; Byte array?
	beq @skip
	cmp #$0A ; String?
	beq @skip
	jmp @done ; No more data to read

@skip:	ldy #$01 ; Setup Copy Engine
	sty $E041
	lda #$D0
	sta $E042
	ldx #$00
	stx $E043
	lda $00
	sta $E044
	lda $D001
	sta $E045
	lda $D001
	sta $E046
	sty $E040 ; Execute Copy Engine Command
	stx $E046 ; Put high byte of size back to 0
	inc $00
	jmp @loop

@done:	copys_up fsclose, $D001, .sizeof(fsclose) ; Call close
	copys_up $0008, $D001, 5
	stx $D001
	stx $D000
	copys_up $0008, $E012, 5 ; Destroy value
	stx $E012
	lda #$04
	sta $E010
	jmp $0200 ; Boot

reset:
	; Display boot greeting
	copys_up greeting, $E003, .sizeof(greeting)

	; Memory Check
	lda $E018
	cmp #$00
	bne havemem
	lda $E019
	cmp #$00
	bne havemem
	; No Memory Installed
	copys_up nomem, $E003, .sizeof(nomem)
@loop:	jmp @loop

havemem:
	copys_up drives, $E003, .sizeof(drives)
	; Look for "drive" components
	copys_up umlist, $E012, .sizeof(umlist)
	lda #$03
	sta $E010

	; Store list to memory
	lda $E011
	sta $03
	readlist 8

	; Parse list
	lda $03
	cmp #$00
	beq fschk ; No "drive" componets left to check
	jsr loaduuid
	copys_up secread, $D001, .sizeof(secread) ; Call readSector
	lda #$00
	sta $D000
	jsr bootdrive
	dec $03

	; Check the drive components

fschk:
	copys_up fsmsg, $E003, .sizeof(fsmsg)
	; Look for "filesystem" components
	copys_up fslist, $E012, .sizeof(fslist)
	lda #$03
	sta $E010

	; Store list to memory
	lda $E011
	sta $03
	readlist 13

	; Parse list
@loop:	lda $03
	cmp #$00
	bne :+
	jmp failboot ; No "filesystem" componets left to check
:	jsr loaduuid
	copys_up fsopend, $D001, .sizeof(fsopend) ; open Thistle/boot
	lda #$00
	sta $D000
	jsr bootfs
	copys_up fsopenf, $D001, .sizeof(fsopenf) ; open Thistle
	lda #$00
	sta $D000
	jsr bootfs
	dec $03
	jmp @loop

.define inputlen $80
.define good $81
.define formattype $82
.define addrmode $83
.define opcode $84
.define indlow $85
.define indhigh $86
.define curlow $87
.define curhigh $88

hex2val:
	; Converts two hexadecimal characters to a value
	lda $00,X
	clc
	sbc #$2f
	cmp #$11
	bcc :+
	clc
	sbc #$06
:	sta good
	dex
	lda $00,X
	clc
	sbc #$2f
	cmp #$11
	bcc :+
	clc
	sbc #$06
:	asl
	asl
	asl
	asl
	eor good
	dex
	sta (curlow),Y
	iny
	rts

inc_y:
	; Handle page wraps
	iny
	cpy #$00
	bne :+
	inc indhigh
:	rts

failboot:
	copys_up noboot, $E003, .sizeof(noboot)
	lda #$00
	sta $E001 ; Drop all input
	sta curlow
	lda #$02
	sta curhigh
@setup:	lda #'$'
	sta $E003
	lda curhigh
	jsr hexprint
	lda curlow
	jsr hexprint
	ldx #' '
	stx $E003
	lda #'>'
	sta $E003
	stx $E003
	lda #$00
	sta inputlen
@loop:	lda $E000
	cmp #$00
	beq @loop ; No Input
	ldx inputlen
	lda $E001
	cmp #$00 ; Scancode, discard
	bne :+
	lda $E001
	jmp @loop
:	cmp #$08 ; Backspace
	bne :+
	cpx #$00
	beq @loop ; No input to delete
	sta $E003
	dec inputlen
	jmp @loop
:	cmp #$0A ; Enter
	beq @exec
	cpx #$80 ; Character
	beq @loop
	sta $E003
	sta $00,X
	inc inputlen
	jmp @loop
@exec:
	sta $E003
	cpx #$00 ; No Input
	beq @setup
	; @setup for comparing cmd names
	lda #<cmdlist
	sta indlow
	lda #>cmdlist
	sta indhigh
	ldx #$00
	ldy #$00
	sty good
	; Compare against cmd list
@ncloop:
	lda (indlow),Y
	cmp #$00 ; NUL, end of cmd name
	beq @nccheck
	cmp $00,X ; Matches
	beq :+
	stx good ; Mark as bad
:	inx
	jsr inc_y
	jmp @ncloop
@nccheck:
	jsr inc_y
	cpx #$00 ; No more cmd entries?
	beq @asm
	cmp good ; A = $00
	bne @ncbad
	cpx inputlen
	beq @ncfound
	lda $00,X
	cmp #' '
	beq @ncfound
@ncbad:
	lda #$00 ; Did not match, reset
	sta good
	ldx #$00
	jsr inc_y ; Skip over address
	jsr inc_y
	jmp @ncloop
@ncfound:
	lda #$4C ; JMP $XXXX
	sta opcode
	lda (indlow),Y
	tax
	jsr inc_y
	lda (indlow),Y
	stx indlow
	sta indhigh
	jsr opcode
	jmp @setup
@asm:
	; Assembly code!
	; Uppercase it all
	ldx #$00
@ucloop:
	lda $00,X
	clc
	sbc #$60
	bmi :+
	adc #$40
	sta $00,X
:	inx
	cpx inputlen
	bne @ucloop
	; @setup for comparing format strings
	ldx #$04
	ldy #$00
	sty formattype
	sty good
	lda inputlen
	cmp #$03 ; Three characters?
	bcs :+
	jmp @badcmd
:	beq @amloop
	lda $03
	cmp #$20
	beq @amloop
	jmp @badcmd ; 4th character not a space
	; Identify addressing mode
@amloop:
	lda format,Y
	cmp #$00 ; NUL, end of format string
	beq @amcheck
	cmp #$5F ; Underscore
	beq :+
	cmp $00,X ; Matches
	beq :+
	stx good ; Mark as bad
:	inx
	iny
	jmp @amloop
@amcheck:
	iny
	cpx #$04 ; No more format entries?
	beq @amfound ; Assume implied
	cmp good ; A = $00
	bne @ambad
	cpx inputlen
	beq @amfound
	bcs @ambad ; Not enough imput
	lda $00,X
	cmp #' '
	beq @amfound
@ambad:
	lda #$00 ; Wrong addressing mode, reset
	sta good
	ldx #$04
	iny
	inc formattype
	jmp @amloop
@amfound:
	lda format,Y
	sta addrmode
	lda #<opcodetbl
	sta indlow
	lda #>opcodetbl
	sta indhigh
	ldy #$00
	ldx #$00
	sty opcode
	sty good
	; Compare opcode names
@oploop:
	lda (indlow),Y
	cmp $00,X
	beq :+
	sta good ; Mark as bad
:	inx
	iny
	cpx #$03
	bne @oploop
	lda (indlow),Y
	cmp addrmode
	beq :+
	stx good ; Mark as bad
:	lda #$00
	cmp good
	beq @opfound
	sta good ; Wrong opcode, reset
	ldx #$00
	jsr inc_y
	inc opcode
	lda opcode
	cmp #$00 ; Wrapped back to 0
	bne :+
	jmp @badop
:	jmp @oploop
@opfound:
	ldy #$00
	lda opcode
	sta (curlow),Y ; Write opcode to memory
	iny
	lda formattype
	cmp #$00 ; immediate
	beq @read21
	cmp #$01 ; immediate w/ Character
	beq @readchar
	cmp #$02 ; absolute
	beq @read40
	cmp #$03 ; zeropage
	beq @read20
	cmp #$04 ; indexed x
	beq @read40
	cmp #$05 ; indexed y
	beq @read40
	cmp #$06 ; zp indexed x
	beq @read20
	cmp #$07 ; zp indexed y
	beq @read20
	cmp #$08 ; indirect
	beq @read41
	cmp #$09 ; indirect x
	beq @read21
	cmp #$0a ; indirect y
	beq @read21
	cmp #$0b ; relative
	beq @read21
	cmp #$0c ; relative w/ minus
	beq @relminus
	jmp @opskip
@readchar:
	ldx #$06
	lda $00,X
	sta (curlow),Y
	jmp @opskip
@read20:
	ldx #$06
	jsr hex2val
	jmp @opskip
@read21:
	ldx #$07
	jsr hex2val
	jmp @opskip
@read40:
	ldx #$08
	jsr hex2val
	jsr hex2val
	jmp @opskip
@read41:
	ldx #$09
	jsr hex2val
	jsr hex2val
	jmp @opskip
@relminus:
	ldx #$08
	jsr hex2val
	dey
	sec
	lda #$00
	sbc (curlow),Y
	sta (curlow),Y
	jmp @opskip
@opskip:
	ldy addrmode
	ldx length,Y
	ldy curlow
@incloop:
	iny
	cpy #$00
	bne :+
	inc curhigh
:	dex
	cpx #$00
	bne @incloop
	sty curlow
	jmp @setup
@badop:
	copys_up unkop, $E003, .sizeof(unkop)
	jmp @setup
@badcmd:
	copys_up unkcmd, $E003, .sizeof(unkcmd)
	jmp @setup

cmd_run:
	jsr $0200
	rts

nmi:
	rti

irq:
	rti

.segment	"VECTORS"

.word nmi
.word reset
.word irq
