;
; Thistle Boot ROM
;
	.setcpu		"65c02"

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
greeting: .byte "Thistle Boot ROM",13,10,10
nomem: .byte "No Memory installed"
drives: .byte "Checking drives ...",13,10
fsmsg: .byte 10,"Checking filesystems ...",13,10
bootmsg: .byte "Booting ...",13,10
noboot: .byte "Nothing to boot from",13,10

hexlookup: .byte "0123456789abcdef"

unkcmd: .byte "Unknown Command",13,10

cmdlist:
.asciiz "ls"
.word cmd_ls
.asciiz "list"
.word cmd_list
.asciiz "load"
.word cmd_load
.asciiz "save"
.word cmd_save
.asciiz "run"
.word cmd_run
.asciiz ""

.define inval $FF

opcodetbl:
.byte "BRK",$07,"ORA",$09,inval,$00,inval,$07,"TSB",$01,"ORA",$01,"ASL",$01,"RB0",$01,"PHP",$07,"ORA",$00,"ASL",$07,inval,$07,"TSB",$04,"ORA",$04,"ASL",$04,"BR0",$0E
.byte "BPL",$08,"ORA",$0A,"ORA",$0D,inval,$07,"TRB",$01,"ORA",$02,"ASL",$02,"RB1",$01,"CLC",$07,"ORA",$06,"INC",$07,inval,$07,"TRB",$04,"ORA",$05,"ASL",$05,"BR1",$0E
.byte "JSR",$04,"AND",$09,inval,$00,inval,$07,"BIT",$01,"AND",$01,"ROL",$01,"RB2",$01,"PLP",$07,"AND",$00,"ROL",$07,inval,$07,"BIT",$04,"AND",$04,"ROL",$04,"BR2",$0E
.byte "BMI",$08,"AND",$0A,"AND",$0D,inval,$07,"BIT",$02,"AND",$02,"ROL",$02,"RB3",$01,"SEC",$07,"AND",$06,"DEC",$07,inval,$07,"BIT",$05,"AND",$05,"ROL",$05,"BR3",$0E
.byte "RTI",$07,"EOR",$09,inval,$00,inval,$07,inval,$01,"EOR",$01,"LSR",$01,"RB4",$01,"PHA",$07,"EOR",$00,"LSR",$07,inval,$07,"JMP",$04,"EOR",$04,"LSR",$04,"BR4",$0E
.byte "BVC",$08,"EOR",$0A,"EOR",$0D,inval,$07,inval,$02,"EOR",$02,"LSR",$02,"RB5",$01,"CLI",$07,"EOR",$06,"PHY",$07,inval,$07,inval,$04,"EOR",$05,"LSR",$05,"BR5",$0E
.byte "RTS",$07,"ADC",$09,inval,$00,inval,$07,"STZ",$01,"ADC",$01,"ROR",$01,"RB6",$01,"PLA",$07,"ADC",$00,"ROR",$07,inval,$07,"JMP",$0B,"ADC",$04,"ROR",$04,"BR6",$0E
.byte "BVS",$08,"ADC",$0A,"ADC",$0D,inval,$07,"STZ",$02,"ADC",$02,"ROR",$02,"RB7",$01,"SEI",$07,"ADC",$06,"PLY",$07,inval,$07,"JMP",$0C,"ADC",$05,"ROR",$05,"BR7",$0E
.byte "BRA",$08,"STA",$09,inval,$00,inval,$07,"STY",$01,"STA",$01,"STX",$01,"SB0",$01,"DEY",$07,"BIT",$00,"TXA",$07,inval,$07,"STY",$04,"STA",$04,"STX",$04,"BS0",$0E
.byte "BCC",$08,"STA",$0A,"STA",$0D,inval,$07,"STY",$02,"STA",$02,"STX",$03,"SB1",$01,"TYA",$07,"STA",$06,"TXS",$07,inval,$07,"STZ",$04,"STA",$05,"STZ",$05,"BS1",$0E
.byte "LDY",$00,"LDA",$09,"LDX",$00,inval,$07,"LDY",$01,"LDA",$01,"LDX",$01,"SB2",$01,"TAY",$07,"LDA",$00,"TAX",$07,inval,$07,"LDY",$04,"LDA",$04,"LDX",$04,"BS2",$0E
.byte "BCS",$08,"LDA",$0A,"LDA",$0D,inval,$07,"LDY",$02,"LDA",$02,"LDX",$03,"SB3",$01,"CLV",$07,"LDA",$06,"TSX",$07,inval,$07,"LDY",$05,"LDA",$05,"LDX",$06,"BS3",$0E
.byte "CPY",$00,"CMP",$09,inval,$00,inval,$07,"CPY",$01,"CMP",$01,"DEC",$01,"SB4",$01,"INY",$07,"CMP",$00,"DEX",$07,"WAI",$07,"CPY",$04,"CMP",$04,"DEC",$04,"BS4",$0E
.byte "BNE",$08,"CMP",$0A,"CMP",$0D,inval,$07,inval,$02,"CMP",$02,"DEC",$02,"SB5",$01,"CLD",$07,"CMP",$06,"PHX",$07,"STP",$07,inval,$04,"CMP",$05,"DEC",$05,"BS5",$0E
.byte "CPX",$00,"SBC",$09,inval,$00,inval,$07,"CPX",$01,"SBC",$01,"INC",$01,"SB6",$01,"INX",$07,"SBC",$00,"NOP",$07,inval,$07,"CPX",$04,"SBC",$04,"INC",$04,"BS6",$0E
.byte "BEQ",$08,"SBC",$0A,"SBC",$0D,inval,$07,inval,$02,"SBC",$02,"INC",$02,"SB7",$01,"SED",$07,"SBC",$06,"PLX",$07,inval,$07,inval,$04,"SBC",$05,"INC",$05,"BS7",$0E

length: .byte 2,2,2,2,3,3,3,1,2,2,2,3,3,2,3

format:
.byte "#$__",$00,$00
.byte "#'_'",$00,$00
.byte "$__",$00,$01
.byte "$__,X",$00,$02
.byte "$__,Y",$00,$03
.byte "$____",$00,$04
.byte "$____,X",$00,$05
.byte "$____,Y",$00,$06
.byte "0X__",$00,$08
.byte "-0X__",$00,$08
.byte "($__,X)",$00,$09
.byte "($__),Y",$00,$0a
.byte "($____)",$00,$0b
.byte "($____,X)",$00,$0c
.byte "($__)",$00,$0d
.byte "$__,0X__",$00,$0e
.byte "$__,-0X__",$00,$0e
.byte $00,$07 ; Must be last

.segment "STARTUP"

.define inputlen $80
.define good $81
.define formattype $82
.define addrmode $83
.define opcode $84
.define indlow $85
.define indhigh $86
.define curlow $87
.define curhigh $88

.macro dmacopy src, dest, len, mode
	.ifnblank src
		.if .const(src) .and .lobyte(src) = 0
			stz $E041
		.else
			lda #<src
			sta $E041
		.endif
		.if .const(src) .and .hibyte(src) = 0
			stz $E042
		.else
			lda #>src
			sta $E042
		.endif
	.endif
	.ifnblank dest
		.if .const(dest) .and .lobyte(dest) = 0
			stz $E043
		.else
			lda #<dest
			sta $E043
		.endif
		.if .const(dest) .and .hibyte(dest) = 0
			stz $E044
		.else
			lda #>dest
			sta $E044
		.endif
	.endif
	.ifnblank len
		.if .const(len) .and len = 0
			stz $E045
		.else
			lda #<len
			sta $E045
		.endif
	.endif
	.if .const(mode) .and mode = 0
		stz $E040
	.else
		lda #mode
		sta $E040
	.endif
.endmacro

.macro dmaload src, dest, len
	.ifnblank src
		.if .const(src) .and .lobyte(src) = 0
			stz $E041
		.else
			lda #<src
			sta $E041
		.endif
		.if .const(src) .and .hibyte(src) = 0
			stz $E042
		.else
			lda #>src
			sta $E042
		.endif
	.endif
	.ifnblank dest
		.if .const(dest) .and .lobyte(dest) = 0
			stz $E043
		.else
			lda #<dest
			sta $E043
		.endif
		.if .const(dest) .and .hibyte(dest) = 0
			stz $E044
		.else
			lda #>dest
			sta $E044
		.endif
	.endif
	.ifnblank len
		.if .const(len) .and len = 0
			stz $E045
		.else
			lda #<len
			sta $E045
		.endif
	.endif
.endmacro

.define mode_pp $00
.define mode_pu $01
.define mode_up $02
.define mode_uu $03

hexprint:
	; Prints a byte to the screen
	; A - byte to print
	; Clobbers: X
	pha
	phy
	tax
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
	ply
	pla
	rts

uuidprint:
	; Prints a UUID to the screen
	; $00, $01 - Address of UUID
	; Clobbers: A, X (hexprint), Y
	ldy #$00
@loop:
	lda (indlow),Y
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
	bra @loop
@dash:
	lda #$2D
	sta $E003
	bra @loop
@done:
	lda #$0d
	sta $E003
	lda #$0a
	sta $E003
	rts

_readlist:
	; Reads component list to $0200
	; $02 - Bytes to skip for component type
	; Clobbers: A, X, Y, indlow, indhigh
	lda $E011
	sta $03
	stz indlow
	ldy #$00
	lda #$02
	sta indhigh
@loop1:
	lda $E012
	cmp #$00 ; TSF End Tag
	beq @done

	; Read UUID
	ldx #$10
@loop2:
	lda $E012
	sta (indlow),Y
	jsr inc_y
	dex
	cpx #$00
	bne @loop2

	; Drop component name
	ldx $02
@loop3:
	lda $E012
	dex
	cpx #$00
	bne @loop3
	bra @loop1
@done:
	lda #$02
	sta indhigh
	rts

.macro readlist skip
	lda #skip
	sta $02
	jsr _readlist
.endmacro

loaduuid:
	; Load a UUID into the component selector buffer
	; indlow, indhigh - Address to read from
	; Clobbers: A, X, Y, (uuidprint)
	jsr uuidprint
loaduuid2:
	ldy indlow
	stz indlow
	ldx #$10
	lda #$0b ; UUID Tag
	sta $E012
@loop:
	lda (indlow),Y ; UUID Byte loop
	sta $E012
	jsr inc_y
	dex
	cpx #$00
	bne @loop
	sty indlow
	stz $E012 ; End Tag
	stz $E010 ; Map Component
	rts

closehandle:
	; Closes an open file handle
	; $08-$0C - Handle to close
	; Clobbers: A
	dmacopy fsclose, $D001, .sizeof(fsclose), mode_up ; Call close
	dmacopy $0008, , 5, mode_up ; ($0008, $D001, 5, mode_up)
	stz $D001
	stz $D000
	dmacopy , $E012, , mode_up ; ($0008, $E012, 5, mode_up) Destroy value
	stz $E012
	lda #$04
	sta $E010
	rts

loadfile:
	; Reads a file into memory starting at $0200
	stz curlow
	lda #$02
	sta curhigh
	dmacopy fsread, $0001, .sizeof(fsread), mode_uu ; Copy read command
	dmacopy $D001, $0008, 5, mode_pu ; Inject handle
@loop:
	ldx curhigh
	cpx #$D0
	beq @done ; Too much data read
	dmacopy $0001, $D001, .sizeof(fsread), mode_up ; Call "read"
	stz $D000

	lda $D001 ; Check TSF Tag
	cmp #$09 ; Byte array?
	beq :+
	cmp #$0A ; String?
	bne @done ; No more data to read

:	lda $D001 ; Read length
	sta indlow
	lda $D001
	sta indhigh
	ldy #$01 ; Setup Copy Engine
	sty $E041
	lda #$D0
	sta $E042
	clc ; Load address and add at same time
	lda curlow
	sta $E043
	adc indlow
	sta curlow
	lda curhigh
	sta $E044
	adc indhigh
	sta curhigh
	lda indlow
	sta $E045
	lda indhigh
	sta $E046
	sty $E040 ; Execute Copy Engine Command
	bra @loop
@done:
	stz $E046 ; Put high byte of size back to 0
	jsr closehandle
	rts

dispboot:
	dmacopy bootmsg, $E003, .sizeof(bootmsg), mode_up
	rts

bootdrive:
	; Checks and boots from a drive
	lda $D000
	cmp #$00
	beq :+
	rts
:	jsr dispboot
	pla ; Remove address from stack
	pla ; We're not returning to havemem
	lda #$01 ; Setup Copy Engine
	sta $E041
	lda #$D0
	sta $E042
	stz $E043
	lda #$02
	sta $E044

	lda $D001 ; Discard tag
	lda $D001
	sta $E045
	lda $D001
	sta $E046

	lda #mode_pu ; Copy
	sta $E040

	stz $E046
	jsr $0200 ; Boot
	jmp fschk

bootfs:
	; Boots from a file
	lda $D000
	cmp #$00
	beq :+
	rts ; No file opened
:	jsr dispboot
	pla ; Remove address from stack
	pla ; We're not returning to fschk
	jsr loadfile
	jsr $0200 ; Boot
	jmp commands

reset:
	; Display boot greeting
	dmacopy greeting, $E003, .sizeof(greeting), mode_up

	; Memory Check
	lda $E018
	cmp #$00
	bne havemem
	lda $E019
	cmp #$00
	bne havemem
	; No Memory Installed
	dmacopy nomem, , .sizeof(nomem), mode_up ; (nomem, $E003, .sizeof(nomem), mode_up)
@loop:	bra @loop

havemem:
	dmacopy drives, , .sizeof(drives), mode_up ; (drives, $E003, .sizeof(drives), mode_up)
	; Look for "drive" components
	dmacopy umlist, $E012, .sizeof(umlist), mode_up
	lda #$03
	sta $E010
	readlist 8 ; Store list to memory

	; Parse list
@loop:
	lda $03
	cmp #$00
	beq fschk ; No "drive" componets left to check
	jsr loaduuid
	dmacopy secread, $D001, .sizeof(secread), mode_up ; Call readSector
	stz $D000
	jsr bootdrive
	dec $03
	bra @loop

fschk:
	dmacopy fsmsg, $E003, .sizeof(fsmsg), mode_up
	; Look for "filesystem" components
	dmacopy fslist, $E012, .sizeof(fslist), mode_up
	lda #$03
	sta $E010
	readlist 13 ; Store list to memory

	; Parse list
@loop:
	lda $03
	cmp #$00
	bne :+
	jmp failboot ; No "filesystem" componets left to check
:	jsr loaduuid
	dmaload , $D001
	dmacopy fsopend, , .sizeof(fsopend), mode_up ; (fsopend, $D001, .sizeof(fsopend), mode_up) open Thistle/boot
	stz $D000
	jsr bootfs
	dmacopy fsopenf, , .sizeof(fsopenf), mode_up ; (fsopenf, $D001, .sizeof(fsopenf), mode_up) open Thistle
	stz $D000
	jsr bootfs
	dec $03
	bra @loop

_hex2val:
	; Converts one hexadecimal characters to a value
	lda $00,X
	clc
	sbc #$2f
	cmp #$11
	bcc :+
	sbc #$07
:	dex
	rts

hex2val:
	; Converts two hexadecimal characters to a value
	jsr _hex2val
	sta good
	jsr _hex2val
	asl
	asl
	asl
	asl
	eor good
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
	dmacopy noboot, $E003, .sizeof(noboot), mode_up

	; Try to select a non tmpfs filesystem
	dmacopy fslist, $E012, .sizeof(fslist), mode_up
	lda #$03
	sta $E010
	readlist 13 ; Store list to memory

@loop:
	lda $03
	cmp #$00
	beq commands
	ldx #$00
	ldy #$00
	stz good
@loop2:
	lda (indlow),Y
	cmp $E110,X
	beq :+
	sty good
	lda indlow
	clc
	adc good
	stx good
	sec
	sbc indlow
	sta indlow
	jsr loaduuid2
	bra commands
:	inx
	jsr inc_y
	cpx #$10
	bne @loop2
	ldx #$00
	dec $03
	bra @loop

commands:
	stz $E001 ; Drop all input
	stz curlow
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
	stz inputlen
@loop:	lda $E000
	cmp #$00
	beq @loop ; No Input
	ldx inputlen
	lda $E001
	cmp #$00 ; Scancode, discard
	bne :+
	lda $E001
	bra @loop
:	cmp #$08 ; Backspace
	bne :+
	cpx #$00
	beq @loop ; No input to delete
	sta $E003
	dec inputlen
	bra @loop
:	cmp #$0D ; Return
	beq @exec
	cpx #$80 ; Character
	beq @loop
	sta $E003
	sta $00,X
	inc inputlen
	bra @loop
@exec:
	sta $E003
	lda #$0A
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
	stz good
	; Compare against cmd list
@ncloop:
	lda (indlow),Y
	cmp #$00 ; NUL, end of cmd name
	beq @nccheck
	cmp $00,X ; Matches
	beq :+
	smb0 good ; Mark as bad
:	inx
	jsr inc_y
	bra @ncloop
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
	stz good ; Did not match, reset
	ldx #$00
	jsr inc_y ; Skip over address
	jsr inc_y
	bra @ncloop
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
	; Setup for comparing format strings
	ldx #$04
	ldy #$00
	stz formattype
	stz good
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
	smb0 good ; Mark as bad
:	inx
	iny
	bra @amloop
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
	stz good ; Wrong addressing mode, reset
	ldx #$04
	iny
	inc formattype
	bra @amloop
@amfound:
	lda format,Y
	sta addrmode
	lda #<opcodetbl
	sta indlow
	lda #>opcodetbl
	sta indhigh
	ldy #$00
	ldx #$00
	stz opcode
	stz good
	; Compare opcode names
@oploop:
	lda (indlow),Y
	cmp $00,X
	beq :+
	smb0 good ; Mark as bad
:	inx
	jsr inc_y
	cpx #$03
	beq @opcheck
	cmp #$FF
	beq @opcheck
	bra @oploop
@opcheck:
	lda (indlow),Y
	cmp addrmode
	beq :+
	smb0 good ; Mark as bad
:	lda #$00
	cmp good
	beq @opfound

	stz good ; Wrong opcode, reset
	ldx #$00
	jsr inc_y
	inc opcode
	lda opcode
	cmp #$00 ; Wrapped back to 0
	bne :+
	jmp @badcmd
:	bra @oploop
@opfound:
	ldy #$00
	lda opcode
	sta (curlow),Y ; Write opcode to memory
	iny
	lda formattype
	cmp #$00 ; immediate
	beq @read21
	cmp #$01 ; immediate w/ character
	beq @readchar
	cmp #$02 ; zeropage
	beq @read20
	cmp #$03 ; zp indexed x
	beq @read20
	cmp #$04 ; zp indexed y
	beq @read20
	cmp #$05 ; absolute
	beq @read40
	cmp #$06 ; indexed x
	beq @read40
	cmp #$07 ; indexed y
	beq @read40
	cmp #$08 ; relative
	beq @read21
	cmp #$09 ; relative w/ minus
	beq @relminus
	cmp #$0a ; zp indirect x
	beq @read21
	cmp #$0b ; zp indirect y
	beq @read21
	cmp #$0c ; indirect
	beq @read41
	cmp #$0d ; indirect x
	beq @read41
	cmp #$0e ; zp indirect
	beq @read21
	cmp #$0f ; zp relative
	beq @readzpr
	cmp #$10 ; zp relative minus
	beq @zprminus
	bra @opskip
@readchar:
	ldx #$06
	lda $00,X
	sta (curlow),Y
	bra @opskip
@read20:
	ldx #$06
	jsr hex2val
	bra @opskip
@read21:
	ldx #$07
	jsr hex2val
	bra @opskip
@read40:
	ldx #$08
	jsr hex2val
	jsr hex2val
	bra @opskip
@read41:
	ldx #$09
	jsr hex2val
	jsr hex2val
	bra @opskip
@readzpr:
	ldx #$06
	jsr hex2val
	ldx #$0b
	jsr hex2val
	bra @opskip
@zprminus:
	ldx #$06
	jsr hex2val
	ldx #$0c
	bra :+
@relminus:
	ldx #$08
:	jsr hex2val
	dey
	sec
	lda #$00
	sbc (curlow),Y
	sta (curlow),Y
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
	bra :+
@badcmd:
	dmacopy unkcmd, $E003, .sizeof(unkcmd), mode_up ; (unkcmd, $E003, .sizeof(unkcmd), mode_up)
:	jmp @setup

loadinput:
	; Loads string from input buffer into Component 1
	lda inputlen
	cmp good
	bcs :+
	lda good
:	clc
	sbc good
	inc
	sta $D001
	sta $E045 ; Copy Engine Length
	stz $D001

	lda good ; Setup Copy Engine
	sta $E041
	stz $E042
	lda #$01
	sta $E043
	lda #$D0
	sta $E044
	lda #mode_up ; Copy
	sta $E040
	rts

.segment "RODATA"

listtest: .byte 10,4,0,"list",10
listfail: .byte "Listing failed",13,10

.segment "STARTUP"

cmd_ls:
	lda #$03 ; inputlen
	sta good
	bra :+
cmd_list:
	lda #$05 ; inputlen
	sta good
:	dmacopy listtest, $D001, .sizeof(listtest), mode_up
	jsr loadinput
	stz $D001 ; TSF End Tag
	stz $D000 ; Invoke
	lda $D000
	cmp #$00
	beq :+
	dmacopy listfail, $E003, .sizeof(listfail), mode_up
	rts
:	lda $D001 ; Drop Array Tag
@loop:
	lda $D001
	cmp #$00 ; TSF End Tag
	beq @done
	ldy $D001 ; Length low
	ldx $D001 ; Length high
@readloop:
	cpy #$00
	bne :+
	cpx #$00
	bne :+
	lda #$0D
	sta $E003
	lda #$0A
	sta $E003
	bra @loop
:	lda $D001
	sta $E003
	dey
	cpy #$FF
	bne :+
	dex
:	bra @readloop
@done:
	rts

.segment "RODATA"

openfail: .byte "Could not open file",13,10
loadmsg: .byte "Loading file ...",13,10

.segment "STARTUP"

cmd_load:
	lda inputlen
	cmp #$06
	bcs :+
	rts
:	dmacopy fsopenf, $D001, 8, mode_up ; Copy 10,4,0,"open",10
	lda #$05 ; inputlen
	sta good
	jsr loadinput
	stz $D001 ; TSF End Tag
	stz $D000 ; Invoke
	lda $D000
	cmp #$00
	beq :+
	dmacopy openfail, $E003, .sizeof(openfail), mode_up ; No file opened
	rts
:	dmacopy loadmsg, $E003, .sizeof(loadmsg), mode_up
	jsr loadfile
	rts

.segment "RODATA"

savemsg: .byte "Saving file ...",13,10
fswrite: .byte 10,5,0,"write"

.segment "STARTUP"

cmd_save:
	lda inputlen
	cmp #$06
	bcs :+
	rts
:	dmacopy fsopenf, $D001, 8, mode_up ; Copy 10,4,0,"open",10
	lda #$05 ; inputlen
	sta good
	jsr loadinput
	lda #10
	sta $D001
	lda #1
	sta $D001
	stz $D001
	lda #'w'
	sta $D001
	stz $D001 ; TSF End Tag
	stz $D000 ; Invoke
	lda $D000
	cmp #$00
	beq :+
	dmacopy openfail, $E003, .sizeof(openfail), mode_up ; No file opened
	rts
:	dmacopy savemsg, $E003, .sizeof(savemsg), mode_up
	dmacopy fswrite, $D001, .sizeof(fswrite), mode_up ; Copy write command
	dmacopy $D001, $0008, 5, mode_pu ; Save handle
	dmacopy $0008, $D001, 5, mode_up
	lda #9
	sta $D001
	ldx curlow
	stx $D001
	ldy curhigh
	dey
	dey
	sty $D001
	stz $E041
	lda #$02 ; Setup Copy Engine
	sta $E042
	lda #$01
	sta $E043
	lda #$D0
	sta $E044
	stx $E045
	sty $E046
	lda #mode_up ; Copy
	sta $E040
	stz $E046 ; Put high byte back to zero
	stz $D001 ; TSF End Tag
	stz $D000 ; Invoke
	jsr closehandle
	rts

cmd_run:
	stz $E005
	jmp $0200

.segment "RODATA"

.byte "]]error",$22,"Thistle architecture required",$22,"--"

.segment "STARTUP"

nmi:
	rti

irq:
	rti

.segment	"VECTORS"

.word nmi
.word reset
.word irq
