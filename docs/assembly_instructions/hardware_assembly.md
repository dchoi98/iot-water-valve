# Hardware Assembly

## 1. Sunshade
1. 3D print sunshade.3mf and sunshade_back.3mf with PETG. White PETG is recommended to reduce heat absorption.
2. Take the JST PH 2-pin cable. Solder extension wires onto the two wires of the cable. Solder the wire connected to the red wire to the positive terminal of the solar panel and the one connected to the black wire to the negative terminal of the solar panel. You will need to cut one of the wires shorter so that they lie perpendicular to the long edge of the solar panel. Cover the solder joints on the solar panel with electrical or duct tape.
3. With the legs of the sunshade facing up, place the solar panel face down into the recess on the sunshade.
4. Thread the JST cable through the sunshade back. Align the hooks on the sunshade back with the holes on the sunshade and press until they snap into place to secure the solar panel.
5. Cut a long, thin piece of electrical or duct tape and wrap it around the JST cable.

## 2. Push-Button Switch
1. Take the JST PH 4-pin cable and the push-button switch.
2. Cut off the header pins on the cable and strip the ends of the wires to expose the cores.
3. Place the push-button switch face down with the electrical leads pointing up. Rotate it so that the text on the plastic is right side up. The lead on the left labelled with a plus sign and the one on the right with a negative sign are the positive and negative leads of the LED. Solder the green wire to the positive lead and the white wire to the negative lead. Solder the red and black wires to the remaining leads (the polarity is not important).

## 3. Enclosure
1. Drill holes into the enclosure with the diameters and positions indicated in enclosure_drawing.pdf.
2. Place the bottom half of the enclosure right side up with the side with three holes facing you.
3. Place the IP67 air vent in the center hole and the PG13.5 cable gland in the rightmost of the three holes. Place them flush against the outer wall and secure them with the included hex nuts. Leave the leftmost hole open for now.
4. Install the two PG11 cable glands into the remaining holes on the other faces.
5. Place the captive screws into the enclosure cover.
6. Align the screw holes of the enclosure cover and the sunshade. Secure the sunshade with four M3x6 screws.

## 4. Electronics
1. To order the PCB, go to https://cart.jlcpcb.com/quote and upload the pcb_gerber.zip file. Choose your preferred color and surface finish. Follow the provided directions and upload the pcb_bom.csv and pcb_cpl.csv files when prompted.
2. 3D print frame.3mf and valve_coupler.3mf. PETG or ABS is recommended.
3. Align the padded holes of the PCB with the screw holes on the frame. The pins on the JST PH connectors should point away from the frame's wall. Secure the PCB with four M3x6 screws.
4. Attach the valve coupler to the servo horn with two M1.7x5 screws. Press the servo horn onto the servo motor axis until it is secure. Manually turn the motor in either direction until it reaches its rotation limit. Remove the servo horn and adjust it so that it is aligned with the long edge of the servo case before pressing it back onto the motor axis.
5. Place the plastic valve into its mounting location on the frame. The rotating part should face toward the frame wall.
6. Place the servo motor on the frame wall with the mounting fins on the side of the PCB and the wires facing toward the mounting location for the charging board. Slide the valve coupler onto the plastic valve. Align the servo and frame screw holes and secure the servo with the included screws. Plug the servo cable into J5 on the PCB.
7. Take the 3-pin JST PH cable and connect the black, red, and white wires to the negative, positive, and signal leads of the water sensor.
8. Place the water sensor in its mounting location on the frame. It should be placed on the side of the attachment point facing the frame wall with the exposed copper lines also facing the frame wall. Secure the water sensor with two M3x10 screws, four lock washers, four flat washers, and two hex nuts. The order of the components should be screw head > lock washer > flat washer > water sensor > frame > flat washer > lock washer > hex nut. Connect the JST PH cable to J6 on the PCB.
9. Cut two 120 mm lengths of 1/4 in tubing and press them onto the openings of the plastic valve until they are secure.
10. Thread the tubes through the PG11 cable glands so that the frame can be placed in the enclosure with PCB closes to the side with three holes.
11. Place the frame into the enclosure and secure it with three of the included M4x8 screws.
12. Connect the cable of the battery to the connector labelled BATT on the charging board. Install the ESP32 and charging boards with the orientations indicated on the PCB.
13. Place the battery in the battery holder with the cable coming out of the top.
14. Thread the cable of the push-button switch through the remaining hole in the enclosure and place the switch flush against the outer wall. Secure the switch with the included hex nut. Connect the cable to J7 on the PCB.
15. If using a USB-C charging cable, thread the cable through the PG13.5 cable gland and connect it to the charging board.
16. Thread the solar panel cable through the PG13.5 cable gland and connect it to J8 on the PCB.
17. Place the enclosure cover onto the enclosure and orient it so that there is as much slack in the solar panel cable as possible. Tighten the captive screws to secure the cover.
18. 3D print gasket.3mf and slide it over the USB-C and solar panel cables. Slide the gasket into the cable gland. Ensure there is a tight fit around the cables and tighten the cable gland.
19. Tighten the remaining cable glands.
##
© 2025. This work is openly licensed via [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/).